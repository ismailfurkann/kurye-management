package com.ivedi.ivedikuryemanagement.report.impl;

import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.report.api.AdminDashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.CourierPerformanceResponse;
import com.ivedi.ivedikuryemanagement.report.api.DashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.DeliveryReportRow;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final BranchRepository branchRepository = mock(BranchRepository.class);
    private final ReportServiceImpl service = new ReportServiceImpl(reportRepository, branchRepository);

    @Test
    void adminDashboard_mapsNullAggregatesToZero() {
        AdminDashboardProjection projection = mock(AdminDashboardProjection.class);
        when(projection.getTotalRestaurants()).thenReturn(1L);
        when(projection.getTotalBranches()).thenReturn(2L);
        when(projection.getTotalCouriers()).thenReturn(3L);
        when(projection.getActiveCouriers()).thenReturn(1L);
        when(projection.getPendingApprovalCouriers()).thenReturn(1L);
        when(projection.getTodayDeliveries()).thenReturn(4L);
        when(projection.getTotalCommission()).thenReturn(null);
        when(projection.getFailedDeliveries()).thenReturn(null);
        when(reportRepository.adminDashboard(any(), any())).thenReturn(projection);

        AdminDashboardStats result = service.adminDashboard();

        assertThat(result.totalRestaurants()).isEqualTo(1);
        assertThat(result.totalCommission()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.failedDeliveries()).isZero();
    }

    @Test
    void managerDashboard_mapsProjection() {
        DashboardProjection projection = mock(DashboardProjection.class);
        when(projection.getTodayOrders()).thenReturn(1L);
        when(projection.getReadyForDispatch()).thenReturn(2L);
        when(projection.getActiveDeliveries()).thenReturn(3L);
        when(projection.getDelivered()).thenReturn(4L);
        when(projection.getFailed()).thenReturn(5L);
        when(projection.getCancelled()).thenReturn(6L);
        when(projection.getActiveCouriers()).thenReturn(7L);
        when(projection.getAverageDeliveryMinutes()).thenReturn(new BigDecimal("22.50"));
        when(reportRepository.managerDashboard(any(), any())).thenReturn(projection);

        DashboardStats result = service.managerDashboard();

        assertThat(result.todayOrders()).isEqualTo(1);
        assertThat(result.averageDeliveryMinutes()).isEqualByComparingTo("22.50");
    }

    @Test
    void deliveryReport_mapsFrontendRows() {
        DeliveryReportProjection projection = mock(DeliveryReportProjection.class);
        UUID orderId = UUID.randomUUID();
        when(projection.getOrderId()).thenReturn(orderId);
        when(projection.getStatus()).thenReturn("DELIVERED");
        when(projection.getOrderAmount()).thenReturn(new BigDecimal("100.00"));
        when(projection.getDeliveryFee()).thenReturn(new BigDecimal("10.00"));
        when(projection.getCommissionAmount()).thenReturn(new BigDecimal("10.00"));
        when(projection.getDeliveryMinutes()).thenReturn(new BigDecimal("31.00"));
        when(reportRepository.deliveryReport(isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        DeliveryReportRow result = service.deliveryReport(null, null, null, null, 0, 20)
                .getContent()
                .getFirst();

        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo(DeliveryOrderStatus.DELIVERED);
        assertThat(result.deliveryMinutes()).isEqualByComparingTo("31.00");
    }

    @Test
    void courierPerformance_mapsFrontendFields() {
        CourierPerformanceProjection projection = mock(CourierPerformanceProjection.class);
        UUID courierId = UUID.randomUUID();
        when(projection.getCourierId()).thenReturn(courierId);
        when(projection.getCourierName()).thenReturn("Ali Kaya");
        when(projection.getVehicleType()).thenReturn("MOTORBIKE");
        when(projection.getRating()).thenReturn(new BigDecimal("4.50"));
        when(projection.getTotalAssignments()).thenReturn(5L);
        when(projection.getDeliveredCount()).thenReturn(4L);
        when(projection.getFailedCount()).thenReturn(1L);
        when(projection.getAvgDeliveryMinutes()).thenReturn(new BigDecimal("27.25"));
        when(reportRepository.courierPerformance(isNull(), isNull(), isNull())).thenReturn(List.of(projection));

        CourierPerformanceResponse result = service.courierPerformanceResponses(null, null).getFirst();

        assertThat(result.courierId()).isEqualTo(courierId);
        assertThat(result.courierName()).isEqualTo("Ali Kaya");
        assertThat(result.vehicleType()).isEqualTo(VehicleType.MOTORBIKE);
        assertThat(result.deliveredCount()).isEqualTo(4);
        assertThat(result.failedCount()).isEqualTo(1);
    }
}
