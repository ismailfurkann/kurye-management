package com.ivedi.ivedikuryemanagement.dispatch.impl;

import com.ivedi.ivedikuryemanagement.commission.api.CommissionService;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.InvalidStatusTransitionException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.CourierService;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderService;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponseMapper;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentService;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final List<AssignmentStatus> ACTIVE_STATUSES =
            List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.PICKED_UP);

    private final DeliveryAssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final CourierService courierService;
    private final DeliveryOrderService deliveryOrderService;
    private final DeliveryAssignmentMapper assignmentMapper;
    private final AssignmentResponseMapper assignmentResponseMapper;
    private final CommissionService commissionService;

    @Override
    public DeliveryAssignmentDto accept(UUID assignmentId) {
        DeliveryAssignment assignment = loadMyAssignment(assignmentId);
        requireStatus(assignment, AssignmentStatus.ASSIGNED);

        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(OffsetDateTime.now());

        log.info("Assignment {} accepted by courier {}", assignmentId, assignment.getCourier().getId());
        return assignmentMapper.toDto(assignmentRepository.save(assignment));
    }

    @Override
    public AssignmentResponse acceptResponse(UUID assignmentId) {
        DeliveryAssignmentDto dto = accept(assignmentId);
        return loadResponse(dto.id());
    }

    @Override
    public DeliveryAssignmentDto reject(UUID assignmentId, String reason) {
        DeliveryAssignment assignment = loadMyAssignment(assignmentId);
        requireStatus(assignment, AssignmentStatus.ASSIGNED);

        UUID courierId = assignment.getCourier().getId();
        UUID orderId = assignment.getDeliveryOrder().getId();

        assignment.setStatus(AssignmentStatus.REJECTED);
        assignment.setCancellationReason(reason);
        assignment.setCancelledAt(OffsetDateTime.now());
        assignmentRepository.save(assignment);

        courierService.decrementActiveCount(courierId);
        deliveryOrderService.transitionStatus(
                orderId, DeliveryOrderStatus.READY_FOR_DISPATCH,
                courierId, "Courier rejected — returning to dispatch queue. Reason: " + reason
        );

        log.info("Assignment {} rejected by courier {}", assignmentId, courierId);
        return assignmentMapper.toDto(assignment);
    }

    @Override
    public AssignmentResponse rejectResponse(UUID assignmentId, String reason) {
        DeliveryAssignmentDto dto = reject(assignmentId, reason);
        return loadResponse(dto.id());
    }

    @Override
    public DeliveryAssignmentDto confirmPickup(UUID assignmentId) {
        DeliveryAssignment assignment = loadMyAssignment(assignmentId);
        requireStatus(assignment, AssignmentStatus.ACCEPTED);

        UUID courierId = assignment.getCourier().getId();
        UUID orderId   = assignment.getDeliveryOrder().getId();

        assignment.setStatus(AssignmentStatus.PICKED_UP);
        assignment.setPickedUpAt(OffsetDateTime.now());
        assignmentRepository.save(assignment);

        deliveryOrderService.transitionStatus(
                orderId, DeliveryOrderStatus.PICKED_UP, courierId, "Courier picked up the order");

        log.info("Assignment {} picked up by courier {}", assignmentId, courierId);
        return assignmentMapper.toDto(assignment);
    }

    @Override
    public AssignmentResponse confirmPickupResponse(UUID assignmentId) {
        DeliveryAssignmentDto dto = confirmPickup(assignmentId);
        return loadResponse(dto.id());
    }

    @Override
    public DeliveryAssignmentDto confirmDelivery(UUID assignmentId) {
        DeliveryAssignment assignment = loadMyAssignment(assignmentId);
        requireStatus(assignment, AssignmentStatus.PICKED_UP);

        UUID courierId = assignment.getCourier().getId();
        UUID orderId   = assignment.getDeliveryOrder().getId();

        assignment.setStatus(AssignmentStatus.DELIVERED);
        assignment.setDeliveredAt(OffsetDateTime.now());
        assignmentRepository.save(assignment);

        deliveryOrderService.transitionStatus(
                orderId, DeliveryOrderStatus.DELIVERED, courierId, "Delivered to customer");
        courierService.decrementActiveCount(courierId);

        // Commission is created in a separate transaction — delivery is not rolled back if commission fails
        try {
            commissionService.createForDelivery(assignmentId);
        } catch (Exception e) {
            log.error("Commission creation failed for assignment {} — requires manual reconciliation", assignmentId, e);
        }

        log.info("Assignment {} delivered by courier {}", assignmentId, courierId);
        return assignmentMapper.toDto(assignment);
    }

    @Override
    public AssignmentResponse confirmDeliveryResponse(UUID assignmentId) {
        DeliveryAssignmentDto dto = confirmDelivery(assignmentId);
        return loadResponse(dto.id());
    }

    @Override
    public DeliveryAssignmentDto reportFailure(UUID assignmentId, String reason) {
        DeliveryAssignment assignment = loadMyAssignment(assignmentId);
        requireStatus(assignment, AssignmentStatus.PICKED_UP);

        UUID courierId = assignment.getCourier().getId();
        UUID orderId   = assignment.getDeliveryOrder().getId();

        assignment.setStatus(AssignmentStatus.FAILED);
        assignment.setFailedAt(OffsetDateTime.now());
        assignment.setFailureReason(reason);
        assignmentRepository.save(assignment);

        deliveryOrderService.transitionStatus(
                orderId, DeliveryOrderStatus.FAILED, courierId, "Delivery failed. Reason: " + reason);
        courierService.decrementActiveCount(courierId);

        log.info("Assignment {} failed by courier {}. Reason: {}", assignmentId, courierId, reason);
        return assignmentMapper.toDto(assignment);
    }

    @Override
    public AssignmentResponse reportFailureResponse(UUID assignmentId, String reason) {
        DeliveryAssignmentDto dto = reportFailure(assignmentId, reason);
        return loadResponse(dto.id());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentDto> getMyActiveAssignments() {
        Courier courier = resolveCurrentCourier();
        return assignmentMapper.toDtoList(
                assignmentRepository.findActiveByCourierId(courier.getId(), ACTIVE_STATUSES)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMyActiveAssignmentResponses() {
        Courier courier = resolveCurrentCourier();
        return assignmentRepository.findActiveByCourierId(courier.getId(), ACTIVE_STATUSES).stream()
                .map(assignmentResponseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAssignmentDto getMyAssignment(UUID assignmentId) {
        return assignmentMapper.toDto(loadMyAssignment(assignmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getMyAssignmentResponse(UUID assignmentId) {
        return assignmentResponseMapper.toResponse(loadMyAssignment(assignmentId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DeliveryAssignment loadMyAssignment(UUID assignmentId) {
        Courier courier = resolveCurrentCourier();
        return assignmentRepository.findByIdAndCourierId(assignmentId, courier.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
    }

    private AssignmentResponse loadResponse(UUID assignmentId) {
        return assignmentRepository.findByIdWithDetails(assignmentId)
                .map(assignmentResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
    }

    private Courier resolveCurrentCourier() {
        UUID userId = currentPrincipal().getId();
        return courierRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new BusinessException(
                        "No courier profile found for current user", "COURIER_PROFILE_NOT_FOUND"
                ));
    }

    private void requireStatus(DeliveryAssignment assignment, AssignmentStatus expected) {
        if (assignment.getStatus() != expected) {
            throw new InvalidStatusTransitionException(assignment.getStatus(), expected);
        }
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
