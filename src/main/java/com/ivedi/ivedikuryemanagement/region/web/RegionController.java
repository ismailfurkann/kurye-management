package com.ivedi.ivedikuryemanagement.region.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.region.api.RegionDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionPermissions;
import com.ivedi.ivedikuryemanagement.region.api.RegionSearchDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionService;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class RegionController {

    private final RegionService regionService;

    // ── Read endpoints — any authenticated user ───────────────────────────────

    @GetMapping("/api/v1/regions")
    @PreAuthorize(RegionPermissions.AUTHENTICATED)
    public ResponseEntity<ApiResponse<PageResponse<RegionDto>>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) RegionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<RegionDto> result = regionService.search(new RegionSearchDto(city, district, status, page, size));
        return ResponseEntity.ok(ApiResponse.success("Regions retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/regions/active")
    @PreAuthorize(RegionPermissions.AUTHENTICATED)
    public ResponseEntity<ApiResponse<List<RegionDto>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success("Active regions retrieved", regionService.getAllActive()));
    }

    @GetMapping("/api/v1/regions/{id}")
    @PreAuthorize(RegionPermissions.AUTHENTICATED)
    public ResponseEntity<ApiResponse<RegionDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Region retrieved", regionService.getById(id)));
    }

    // ── Admin write endpoints — PLATFORM_ADMIN only ──────────────────────────

    @PostMapping("/api/v1/regions")
    @PreAuthorize(RegionPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RegionDto>> createAlias(@Valid @RequestBody RegionRequest request) {
        RegionDto created = regionService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Region created", created));
    }

    @PutMapping("/api/v1/regions/{id}")
    @PreAuthorize(RegionPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RegionDto>> updateAlias(
            @PathVariable UUID id,
            @Valid @RequestBody RegionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Region updated", regionService.update(id, request)));
    }

    @PatchMapping("/api/v1/regions/{id}/toggle-status")
    @PreAuthorize(RegionPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RegionDto>> toggleStatusAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Region status updated", regionService.toggleStatus(id)));
    }

    @PostMapping("/api/v1/admin/regions")
    @PreAuthorize(RegionPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RegionDto>> create(@Valid @RequestBody RegionRequest request) {
        RegionDto created = regionService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Region created", created));
    }

    @PatchMapping("/api/v1/admin/regions/{id}")
    @PreAuthorize(RegionPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RegionDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RegionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Region updated", regionService.update(id, request)));
    }

    @PatchMapping("/api/v1/admin/regions/{id}/status")
    @PreAuthorize(RegionPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RegionDto>> setStatus(
            @PathVariable UUID id,
            @Valid @RequestBody RegionStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Region status updated", regionService.setStatus(id, request)));
    }
}
