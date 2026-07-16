package com.ivedi.ivedikuryemanagement.commission.web;

import com.ivedi.ivedikuryemanagement.commission.api.CommissionDto;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionResponse;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionService;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionSummaryResponse;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionStatus;
import com.ivedi.ivedikuryemanagement.commission.api.EarningsSummaryDto;
import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class CommissionController {

    private final CommissionService commissionService;

    // ── Courier self ──────────────────────────────────────────────────────────

    @GetMapping("/api/v1/courier/commissions")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<PageResponse<CommissionDto>>> getMyCommissions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Commissions retrieved",
                PageResponse.from(commissionService.getMyCommissions(page, size))));
    }

    @GetMapping("/api/v1/courier/commissions/summary")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<EarningsSummaryDto>> getMyEarningsSummary(
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Period must be in YYYY-MM format")
            String period
    ) {
        return ResponseEntity.ok(ApiResponse.success("Earnings summary retrieved",
                commissionService.getMyEarningsSummary(period)));
    }

    @GetMapping("/api/v1/commissions/me")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<PageResponse<CommissionResponse>>> getMyCommissionsAlias(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Commissions retrieved",
                PageResponse.from(commissionService.getMyCommissionResponses(page, size))));
    }

    @GetMapping("/api/v1/commissions/me/summary")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<ApiResponse<CommissionSummaryResponse>> getMySummaryAlias() {
        return ResponseEntity.ok(ApiResponse.success("Commission summary retrieved",
                commissionService.getMyFrontendSummary()));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/commissions")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<CommissionDto>>> search(
            @RequestParam(required = false) UUID courierId,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Period must be in YYYY-MM format")
            String period,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Commissions retrieved",
                PageResponse.from(commissionService.search(courierId, status, period, page, size))));
    }

    @GetMapping("/api/v1/commissions")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CommissionResponse>>> searchAlias(
            @RequestParam(required = false) UUID courierId,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Period must be in YYYY-MM format")
            String period,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Commissions retrieved",
                PageResponse.from(commissionService.searchResponses(courierId, status, period, page, size))));
    }

    @GetMapping("/api/v1/commissions/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<CommissionResponse>> getByIdAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Commission retrieved",
                commissionService.getResponseById(id)));
    }

    @PostMapping("/api/v1/commissions/{id}/pay")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<CommissionResponse>> payOneAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Commission marked as PAID",
                commissionService.markOneAsPaid(id)));
    }

    @PostMapping("/api/v1/admin/commissions/pay")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> markAsPaid(
            @RequestParam UUID courierId,
            @RequestParam
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Period must be in YYYY-MM format")
            String period
    ) {
        int updated = commissionService.markAsPaid(courierId, period);
        return ResponseEntity.ok(ApiResponse.success(updated + " commissions marked as PAID", updated));
    }
}
