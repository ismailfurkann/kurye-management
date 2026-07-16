package com.ivedi.ivedikuryemanagement.report.web;

import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.report.api.AdminDashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.CourierPerformanceResponse;
import com.ivedi.ivedikuryemanagement.report.api.DashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.DeliveryReportRow;
import com.ivedi.ivedikuryemanagement.report.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAliasControllerTest {

    private final ReportServiceImpl reportService = mock(ReportServiceImpl.class);
    private final ReportAliasController controller = new ReportAliasController(reportService);

    @Test
    void dashboards_delegateAndReturnFrontendDtos() {
        AdminDashboardStats admin = new AdminDashboardStats(1, 2, 3, 1, 1, 4, BigDecimal.TEN, 0);
        DashboardStats manager = dashboard();
        DashboardStats restaurant = dashboard();
        UUID branchId = UUID.randomUUID();
        when(reportService.adminDashboard()).thenReturn(admin);
        when(reportService.managerDashboard()).thenReturn(manager);
        when(reportService.restaurantDashboard(branchId)).thenReturn(restaurant);

        assertThat(controller.adminDashboard().getBody().getData()).isEqualTo(admin);
        assertThat(controller.managerDashboard().getBody().getData()).isEqualTo(manager);
        assertThat(controller.restaurantDashboard(branchId).getBody().getData()).isEqualTo(restaurant);
    }

    @Test
    void deliveryReport_returnsPageResponseAndPassesFilters() {
        UUID restaurantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);
        DeliveryReportRow row = deliveryRow(restaurantId, branchId);
        when(reportService.deliveryReport(branchId, restaurantId, start, end, 0, 20))
                .thenReturn(new PageImpl<>(List.of(row)));

        PageResponse<DeliveryReportRow> page = controller
                .deliveryReport(start, end, restaurantId, branchId, 0, 20)
                .getBody()
                .getData();

        assertThat(page.content()).containsExactly(row);
        assertThat(page.totalElements()).isEqualTo(1);
        verify(reportService).deliveryReport(branchId, restaurantId, start, end, 0, 20);
    }

    @Test
    void courierPerformance_returnsFrontendRows() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);
        CourierPerformanceResponse row = new CourierPerformanceResponse(
                UUID.randomUUID(), "Ali Kaya", VehicleType.MOTORBIKE, BigDecimal.ZERO, 3, 2, 1, new BigDecimal("25.50"));
        when(reportService.courierPerformanceResponses(start, end)).thenReturn(List.of(row));

        assertThat(controller.courierPerformance(start, end).getBody().getData()).containsExactly(row);
    }

    private DashboardStats dashboard() {
        return new DashboardStats(1, 2, 3, 4, 5, 6, 7, new BigDecimal("18.25"));
    }

    private DeliveryReportRow deliveryRow(UUID restaurantId, UUID branchId) {
        return new DeliveryReportRow(
                UUID.randomUUID(),
                "ORD-1",
                restaurantId,
                "Restaurant",
                branchId,
                "Branch",
                UUID.randomUUID(),
                "Courier",
                DeliveryOrderStatus.DELIVERED,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                new BigDecimal("30.00")
        );
    }
}
