package com.ivedi.ivedikuryemanagement.report.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.report.api.AdminDashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.CourierPerformanceResponse;
import com.ivedi.ivedikuryemanagement.report.api.DashboardStats;
import com.ivedi.ivedikuryemanagement.report.api.DeliveryReportRow;
import com.ivedi.ivedikuryemanagement.report.impl.ReportServiceImpl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class ReportAliasController {

    private final ReportServiceImpl reportService;

    @GetMapping("/api/v1/reports/admin/dashboard")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> adminDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard retrieved",
                reportService.adminDashboard()));
    }

    @GetMapping("/api/v1/reports/manager/dashboard")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')")
    public ResponseEntity<ApiResponse<DashboardStats>> managerDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Manager dashboard retrieved",
                reportService.managerDashboard()));
    }

    @GetMapping("/api/v1/reports/restaurant/dashboard")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<DashboardStats>> restaurantDashboard(
            @RequestParam(required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant dashboard retrieved",
                reportService.restaurantDashboard(branchId)));
    }

    @GetMapping("/api/v1/reports/deliveries")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryReportRow>>> deliveryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Delivery report retrieved",
                PageResponse.from(reportService.deliveryReport(branchId, restaurantId, startDate, endDate, page, size))));
    }

    @GetMapping("/api/v1/reports/courier-performance")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')")
    public ResponseEntity<ApiResponse<List<CourierPerformanceResponse>>> courierPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.success("Courier performance retrieved",
                reportService.courierPerformanceResponses(startDate, endDate)));
    }
}
