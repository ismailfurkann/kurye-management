package com.ivedi.ivedikuryemanagement.commission.impl;

import com.ivedi.ivedikuryemanagement.commission.api.*;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignment;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionServiceImpl implements CommissionService {

    private static final BigDecimal DEFAULT_RATE = BigDecimal.ONE; // 100% of delivery_fee to courier

    private final CommissionRepository commissionRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final CommissionMapper commissionMapper;
    private final CommissionResponseMapper commissionResponseMapper;

    /**
     * REQUIRES_NEW — commission failure must never roll back the delivery transaction.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommissionDto createForDelivery(UUID assignmentId) {
        DeliveryAssignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAssignment", assignmentId));

        BigDecimal gross = assignment.getDeliveryOrder().getDeliveryFee();
        BigDecimal net   = gross.multiply(DEFAULT_RATE).setScale(2, RoundingMode.HALF_UP);
        String period    = YearMonth.now().toString(); // YYYY-MM

        Commission commission = Commission.builder()
                .deliveryAssignment(assignment)
                .deliveryOrder(assignment.getDeliveryOrder())
                .courier(assignment.getCourier())
                .grossAmount(gross)
                .commissionRate(DEFAULT_RATE)
                .netAmount(net)
                .status(CommissionStatus.PENDING)
                .paymentPeriod(period)
                .build();

        Commission saved = commissionRepository.save(commission);
        log.info("Commission {} created for assignment {} — net={}", saved.getId(), assignmentId, net);
        return commissionMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionDto> getMyCommissions(int page, int size) {
        Courier courier = resolveCurrentCourier();
        return commissionRepository.findByCourierId(
                courier.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ).map(commissionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getMyCommissionResponses(int page, int size) {
        Courier courier = resolveCurrentCourier();
        return commissionRepository.findByCourierId(
                courier.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ).map(commissionResponseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EarningsSummaryDto getMyEarningsSummary(String period) {
        Courier courier = resolveCurrentCourier();
        return buildSummary(courier.getId(), period);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponse getMyFrontendSummary() {
        Courier courier = resolveCurrentCourier();
        String currentPeriod = YearMonth.now().toString();
        CommissionSummaryProjection projection =
                commissionRepository.aggregateFrontendSummary(courier.getId(), currentPeriod);
        return new CommissionSummaryResponse(
                toLong(projection != null ? projection.getTotalDeliveries() : null),
                toBd(projection != null ? projection.getPendingAmount() : null),
                toBd(projection != null ? projection.getPaidAmount() : null),
                toBd(projection != null ? projection.getTotalAmount() : null),
                toBd(projection != null ? projection.getCurrentPeriodAmount() : null)
        );
    }

    @Override
    @Transactional
    public int markAsPaid(UUID courierId, String period) {
        int updated = commissionRepository.markAsPaid(courierId, period, OffsetDateTime.now());
        log.info("Marked {} commissions as PAID for courier {} period {}", updated, courierId, period);
        return updated;
    }

    @Override
    @Transactional
    public CommissionResponse markOneAsPaid(UUID id) {
        Commission commission = commissionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commission", id));
        if (commission.getStatus() != CommissionStatus.PENDING) {
            throw new BusinessException("Only PENDING commissions can be paid", "COMMISSION_NOT_PAYABLE");
        }
        int updated = commissionRepository.markOneAsPaid(id, OffsetDateTime.now());
        if (updated != 1) {
            throw new BusinessException("Commission could not be paid", "COMMISSION_NOT_PAYABLE");
        }
        log.info("Commission {} marked as PAID", id);
        return commissionResponseMapper.toResponse(
                commissionRepository.findByIdWithDetails(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Commission", id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionDto> search(UUID courierId, CommissionStatus status, String period, int page, int size) {
        return commissionRepository.search(
                courierId, status, period,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ).map(commissionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> searchResponses(UUID courierId, CommissionStatus status, String period, int page, int size) {
        return commissionRepository.search(
                courierId, status, period,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ).map(commissionResponseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionResponse getResponseById(UUID id) {
        return commissionResponseMapper.toResponse(
                commissionRepository.findByIdWithDetails(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Commission", id))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EarningsSummaryDto buildSummary(UUID courierId, String period) {
        EarningsSummaryProjection r = commissionRepository.aggregateEarnings(courierId, period);
        if (r == null || r.getTotalCount() == null) {
            return new EarningsSummaryDto(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }
        return new EarningsSummaryDto(
                toLong(r.getTotalCount()),
                toBd(r.getGrossTotal()),
                toBd(r.getNetTotal()),
                toLong(r.getPendingCount()),
                toBd(r.getPendingTotal()),
                toLong(r.getPaidCount()),
                toBd(r.getPaidTotal())
        );
    }

    private Courier resolveCurrentCourier() {
        UUID userId = currentPrincipal().getId();
        return courierRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new BusinessException(
                        "No courier profile found for current user", "COURIER_PROFILE_NOT_FOUND"
                ));
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private BigDecimal toBd(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
