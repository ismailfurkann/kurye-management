package com.ivedi.ivedikuryemanagement.branch.web;

import com.ivedi.ivedikuryemanagement.branch.api.BranchDto;
import com.ivedi.ivedikuryemanagement.branch.api.BranchPermissions;
import com.ivedi.ivedikuryemanagement.branch.api.BranchResponse;
import com.ivedi.ivedikuryemanagement.branch.api.BranchSearchDto;
import com.ivedi.ivedikuryemanagement.branch.api.BranchService;
import com.ivedi.ivedikuryemanagement.branch.api.BranchStatus;
import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
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
public class BranchController {

    private final BranchService branchService;

    // ── Admin write endpoints ─────────────────────────────────────────────────

    @PostMapping("/api/v1/admin/branches")
    @PreAuthorize(BranchPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<BranchDto>> create(
            @Valid @RequestBody BranchRequest request
    ) {
        BranchDto created = branchService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created", created));
    }

    @PatchMapping("/api/v1/admin/branches/{id}")
    @PreAuthorize(BranchPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<BranchDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BranchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Branch updated", branchService.update(id, request)));
    }

    @PatchMapping("/api/v1/admin/branches/{id}/status")
    @PreAuthorize(BranchPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<BranchDto>> setStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BranchStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Branch status updated", branchService.setStatus(id, request)));
    }

    @PostMapping("/api/v1/restaurants/{restaurantId}/branches")
    @PreAuthorize(BranchPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<BranchResponse>> createAlias(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody BranchRequest request
    ) {
        BranchRequest pathBackedRequest = new BranchRequest(
                restaurantId,
                request.regionId(),
                request.name(),
                request.phone(),
                request.addressText(),
                request.latitude(),
                request.longitude()
        );
        BranchResponse created = branchService.createResponse(pathBackedRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created", created));
    }

    @PutMapping("/api/v1/branches/{id}")
    @PreAuthorize(BranchPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<BranchResponse>> updateAlias(
            @PathVariable UUID id,
            @Valid @RequestBody BranchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Branch updated", branchService.updateResponse(id, request)));
    }

    @PatchMapping("/api/v1/branches/{id}/toggle-status")
    @PreAuthorize(BranchPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<BranchResponse>> toggleStatusAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Branch status updated", branchService.toggleStatus(id)));
    }

    // ── Admin / Manager read endpoints ────────────────────────────────────────

    @GetMapping("/api/v1/admin/branches")
    @PreAuthorize(BranchPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<PageResponse<BranchResponse>>> search(
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<BranchResponse> result = branchService.searchResponses(
                new BranchSearchDto(restaurantId, regionId, name, status, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/branches")
    @PreAuthorize(BranchPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<PageResponse<BranchResponse>>> searchAlias(
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<BranchResponse> result = branchService.searchResponses(
                new BranchSearchDto(restaurantId, null, search, null, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/admin/branches/{id}")
    @PreAuthorize(BranchPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<BranchDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved", branchService.getById(id)));
    }

    @GetMapping("/api/v1/branches/{id}")
    @PreAuthorize(BranchPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<BranchResponse>> getByIdAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved", branchService.getResponseById(id)));
    }

    @GetMapping("/api/v1/admin/restaurants/{restaurantId}/branches")
    @PreAuthorize(BranchPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<List<BranchDto>>> getByRestaurant(
            @PathVariable UUID restaurantId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Branches retrieved", branchService.getByRestaurantId(restaurantId))
        );
    }

    @GetMapping("/api/v1/restaurants/{restaurantId}/branches")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER', 'RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getByRestaurantAlias(
            @PathVariable UUID restaurantId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Branches retrieved", branchService.getByRestaurantIdResponsesForCurrentUser(restaurantId))
        );
    }

    @GetMapping("/api/v1/branches/me")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getMyBranchesAlias() {
        return ResponseEntity.ok(
                ApiResponse.success("Your branches retrieved", branchService.getMyBranchResponses())
        );
    }

    // ── Owner endpoints — ownership enforced at service layer ─────────────────

    @GetMapping("/api/v1/restaurants/mine/{restaurantId}/branches")
    @PreAuthorize(BranchPermissions.OWNER_ONLY)
    public ResponseEntity<ApiResponse<List<BranchDto>>> getMyBranches(
            @PathVariable UUID restaurantId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Your branches retrieved", branchService.getMyBranches(restaurantId))
        );
    }

    @GetMapping("/api/v1/restaurants/mine/{restaurantId}/branches/{branchId}")
    @PreAuthorize(BranchPermissions.OWNER_ONLY)
    public ResponseEntity<ApiResponse<BranchDto>> getMyBranchById(
            @PathVariable UUID restaurantId,
            @PathVariable UUID branchId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Branch retrieved", branchService.getMyBranchById(restaurantId, branchId))
        );
    }
}
