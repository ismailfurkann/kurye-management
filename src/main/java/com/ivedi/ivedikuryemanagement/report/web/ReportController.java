package com.ivedi.ivedikuryemanagement.report.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.report.api.CourierPerformanceDto;
import com.ivedi.ivedikuryemanagement.report.api.DeliveryStatsDto;
import com.ivedi.ivedikuryemanagement.report.impl.ReportServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceImpl reportService;

    @GetMapping("/deliveries")
    public ResponseEntity<ApiResponse<DeliveryStatsDto>> deliveryStats(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(ApiResponse.success("Delivery stats retrieved",
                reportService.deliveryStats(branchId, restaurantId, dateFrom, dateTo)));
    }

    @GetMapping("/couriers/performance")
    public ResponseEntity<ApiResponse<List<CourierPerformanceDto>>> courierPerformance(
            @RequestParam(required = false) UUID courierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(ApiResponse.success("Courier performance retrieved",
                reportService.courierPerformance(courierId, dateFrom, dateTo)));
    }
}
