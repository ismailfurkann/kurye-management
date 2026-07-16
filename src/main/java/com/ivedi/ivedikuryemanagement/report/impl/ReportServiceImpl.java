package com.ivedi.ivedikuryemanagement.report.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.exception.UnauthorizedException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.report.api.AdminDashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.CourierPerformanceResponse;
import com.ivedi.ivedikuryemanagement.report.api.DashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.DeliveryReportRow;
import com.ivedi.ivedikuryemanagement.report.api.CourierPerformanceDto;
import com.ivedi.ivedikuryemanagement.report.api.DeliveryStatsDto;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl {

    private final ReportRepository reportRepository;
    private final BranchRepository branchRepository;

    public DeliveryStatsDto deliveryStats(UUID branchId, UUID restaurantId,
                                          LocalDate dateFrom, LocalDate dateTo) {
        validateDateRange(dateFrom, dateTo);
        OffsetDateTime from = dateFrom != null
                ? dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime to = dateTo != null
                ? dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;

        List<StatusAggregateProjection> rows = reportRepository.aggregateByStatus(branchId, restaurantId, from, to);

        Map<String, long[]> counts = new HashMap<>(); // status → [count, fee*100]
        BigDecimal totalFee = BigDecimal.ZERO;
        long total = 0;

        for (StatusAggregateProjection row : rows) {
            String status = row.getStatus();
            long cnt = toLong(row.getCnt());
            BigDecimal fee = toBd(row.getTotalFee());
            counts.put(status, new long[]{cnt});
            total += cnt;
            totalFee = totalFee.add(fee);
        }

        long delivered  = count(counts, "DELIVERED");
        long cancelled  = count(counts, "CANCELLED");
        long failed     = count(counts, "FAILED");
        double successRate = total > 0
                ? Math.round((delivered * 100.0 / total) * 10) / 10.0
                : 0.0;

        return new DeliveryStatsDto(
                total,
                count(counts, "CREATED"),
                count(counts, "READY_FOR_DISPATCH"),
                count(counts, "ASSIGNED"),
                count(counts, "PICKED_UP"),
                delivered,
                cancelled,
                failed,
                totalFee,
                successRate
        );
    }

    public List<CourierPerformanceDto> courierPerformance(UUID courierId,
                                                           LocalDate dateFrom, LocalDate dateTo) {
        validateDateRange(dateFrom, dateTo);
        OffsetDateTime from = dateFrom != null
                ? dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime to = dateTo != null
                ? dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;

        return reportRepository.courierPerformance(courierId, from, to)
                .stream()
                .map(r -> {
                    long total     = toLong(r.getTotalAssignments());
                    long delivered = toLong(r.getDeliveredCount());
                    double rate    = total > 0
                            ? Math.round((delivered * 100.0 / total) * 10) / 10.0 : 0.0;
                    return new CourierPerformanceDto(
                            r.getCourierId(),
                            r.getCourierName(),
                            total,
                            delivered,
                            0,
                            toLong(r.getFailedCount()),
                            rate,
                            BigDecimal.ZERO,
                            toBd(r.getRating())
                    );
                })
                .toList();
    }

    public AdminDashboardStats adminDashboard() {
        OffsetDateTime todayStart = todayStart();
        AdminDashboardProjection p = reportRepository.adminDashboard(todayStart, todayStart.plusDays(1));
        return new AdminDashboardStats(
                toLong(p.getTotalRestaurants()),
                toLong(p.getTotalBranches()),
                toLong(p.getTotalCouriers()),
                toLong(p.getActiveCouriers()),
                toLong(p.getPendingApprovalCouriers()),
                toLong(p.getTodayDeliveries()),
                toBd(p.getTotalCommission()),
                toLong(p.getFailedDeliveries())
        );
    }

    public DashboardStats managerDashboard() {
        OffsetDateTime todayStart = todayStart();
        return toDashboardStats(reportRepository.managerDashboard(todayStart, todayStart.plusDays(1)));
    }

    public DashboardStats restaurantDashboard(UUID branchId) {
        UserPrincipal principal = currentPrincipal();
        if (principal.getRole() == UserRole.RESTAURANT_STAFF) {
            throw new UnauthorizedException("Restaurant staff branch scope is not configured");
        }
        if (principal.getRole() == UserRole.RESTAURANT_OWNER && branchId != null) {
            Branch branch = branchRepository.findByIdWithDetails(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
            if (!branch.getRestaurant().getOwner().getId().equals(principal.getId())) {
                throw new UnauthorizedException("You do not have access to this branch");
            }
        }
        OffsetDateTime todayStart = todayStart();
        return toDashboardStats(reportRepository.restaurantDashboard(principal.getId(), branchId, todayStart, todayStart.plusDays(1)));
    }

    public Page<DeliveryReportRow> deliveryReport(UUID branchId, UUID restaurantId,
                                                   LocalDate startDate, LocalDate endDate,
                                                   int page, int size) {
        validateDateRange(startDate, endDate);
        OffsetDateTime from = startDate != null ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime to = endDate != null ? endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        return reportRepository.deliveryReport(
                branchId,
                restaurantId,
                from,
                to,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ).map(this::toDeliveryReportRow);
    }

    public List<CourierPerformanceResponse> courierPerformanceResponses(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        OffsetDateTime from = startDate != null ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime to = endDate != null ? endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        return reportRepository.courierPerformance(null, from, to).stream()
                .map(this::toCourierPerformanceResponse)
                .toList();
    }

    private DashboardStats toDashboardStats(DashboardProjection p) {
        return new DashboardStats(
                toLong(p.getTodayOrders()),
                toLong(p.getReadyForDispatch()),
                toLong(p.getActiveDeliveries()),
                toLong(p.getDelivered()),
                toLong(p.getFailed()),
                toLong(p.getCancelled()),
                toLong(p.getActiveCouriers()),
                toBd(p.getAverageDeliveryMinutes())
        );
    }

    private DeliveryReportRow toDeliveryReportRow(DeliveryReportProjection p) {
        return new DeliveryReportRow(
                p.getOrderId(),
                p.getOrderCode(),
                p.getRestaurantId(),
                p.getRestaurantName(),
                p.getBranchId(),
                p.getBranchName(),
                p.getCourierId(),
                p.getCourierName(),
                p.getStatus() != null ? DeliveryOrderStatus.valueOf(p.getStatus()) : null,
                toBd(p.getOrderAmount()),
                toBd(p.getDeliveryFee()),
                toBd(p.getCommissionAmount()),
                p.getCreatedAt(),
                p.getDeliveredAt(),
                toBd(p.getDeliveryMinutes())
        );
    }

    private CourierPerformanceResponse toCourierPerformanceResponse(CourierPerformanceProjection p) {
        return new CourierPerformanceResponse(
                p.getCourierId(),
                p.getCourierName(),
                p.getVehicleType() != null ? VehicleType.valueOf(p.getVehicleType()) : null,
                toBd(p.getRating()),
                toLong(p.getTotalAssignments()),
                toLong(p.getDeliveredCount()),
                toLong(p.getFailedCount()),
                toBd(p.getAvgDeliveryMinutes())
        );
    }

    private long count(Map<String, long[]> m, String key) {
        long[] v = m.get(key);
        return v == null ? 0L : v[0];
    }

    private long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private BigDecimal toBd(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private OffsetDateTime todayStart() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException("dateFrom must be before or equal to dateTo", "INVALID_DATE_RANGE");
        }
    }
}
