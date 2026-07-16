package com.ivedi.ivedikuryemanagement.courier.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.api.CourierDto;
import com.ivedi.ivedikuryemanagement.courier.api.CourierPermissions;
import com.ivedi.ivedikuryemanagement.courier.api.CourierResponse;
import com.ivedi.ivedikuryemanagement.courier.api.CourierSearchDto;
import com.ivedi.ivedikuryemanagement.courier.api.CourierService;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
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
public class CourierController {

    private final CourierService courierService;

    // ── Courier self endpoints ─────────────────────────────────────────────────

    @PostMapping("/api/v1/courier/profile")
    @PreAuthorize(CourierPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<CourierDto>> createProfile(
            @Valid @RequestBody CourierRegistrationRequest request
    ) {
        CourierDto created = courierService.createProfile(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Courier profile created — awaiting admin approval", created));
    }

    @GetMapping("/api/v1/courier/profile")
    @PreAuthorize(CourierPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<CourierDto>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", courierService.getMyProfile()));
    }

    @PatchMapping("/api/v1/courier/availability")
    @PreAuthorize(CourierPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<CourierDto>> updateAvailability(
            @Valid @RequestBody CourierAvailabilityRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Availability updated", courierService.updateAvailability(request))
        );
    }

    @PatchMapping("/api/v1/courier/location")
    @PreAuthorize(CourierPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<CourierDto>> updateLocation(
            @Valid @RequestBody CourierLocationRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Location updated", courierService.updateLocation(request))
        );
    }

    @GetMapping("/api/v1/couriers/me")
    @PreAuthorize(CourierPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<CourierResponse>> getMeAlias() {
        return ResponseEntity.ok(ApiResponse.success("Courier retrieved", courierService.getMyProfileResponse()));
    }

    @PatchMapping("/api/v1/couriers/me/availability")
    @PreAuthorize(CourierPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<CourierResponse>> updateAvailabilityAlias(
            @Valid @RequestBody CourierAvailabilityRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Availability updated", courierService.updateAvailabilityResponse(request))
        );
    }

    @PostMapping("/api/v1/couriers/me/location")
    @PreAuthorize(CourierPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<CourierResponse>> updateLocationAlias(
            @Valid @RequestBody CourierLocationRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Location updated", courierService.updateLocationResponse(request))
        );
    }

    // ── Admin / Manager read endpoints ────────────────────────────────────────

    @GetMapping("/api/v1/admin/couriers")
    @PreAuthorize(CourierPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<PageResponse<CourierDto>>> search(
            @RequestParam(required = false) AvailabilityStatus availabilityStatus,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<CourierDto> result = courierService.search(
                new CourierSearchDto(availabilityStatus, approvalStatus, vehicleType, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success("Couriers retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/couriers")
    @PreAuthorize(CourierPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<PageResponse<CourierResponse>>> searchAlias(
            @RequestParam(required = false) AvailabilityStatus availability,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<CourierResponse> result = courierService.searchResponses(
                new CourierSearchDto(availability, approvalStatus, vehicleType, regionId, search, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success("Couriers retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/admin/couriers/{id}")
    @PreAuthorize(CourierPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<CourierDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Courier retrieved", courierService.getById(id)));
    }

    @GetMapping("/api/v1/couriers/{id}")
    @PreAuthorize(CourierPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<CourierResponse>> getByIdAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Courier retrieved", courierService.getResponseById(id)));
    }

    // ── Admin write endpoints ─────────────────────────────────────────────────

    @PatchMapping("/api/v1/admin/couriers/{id}/approval")
    @PreAuthorize(CourierPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<CourierDto>> setApprovalStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CourierApprovalRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Courier approval status updated", courierService.setApprovalStatus(id, request))
        );
    }

    @PostMapping("/api/v1/couriers/{id}/approve")
    @PreAuthorize(CourierPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<CourierResponse>> approveAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Courier approved",
                courierService.setApprovalStatusResponse(id, new CourierApprovalRequest(ApprovalStatus.APPROVED))
        ));
    }

    @PostMapping("/api/v1/couriers/{id}/reject")
    @PreAuthorize(CourierPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<CourierResponse>> rejectAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Courier rejected",
                courierService.setApprovalStatusResponse(id, new CourierApprovalRequest(ApprovalStatus.REJECTED))
        ));
    }

    @PostMapping("/api/v1/couriers/{id}/block")
    @PreAuthorize(CourierPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<CourierResponse>> blockAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Courier blocked",
                courierService.setApprovalStatusResponse(id, new CourierApprovalRequest(ApprovalStatus.BLOCKED))
        ));
    }

    @GetMapping("/api/v1/couriers/{id}/assignments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER', 'COURIER')")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryAssignmentDto>>> getAssignmentsAlias(
            @PathVariable UUID id,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<DeliveryAssignmentDto> result = courierService.getAssignments(id, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Courier assignments retrieved", PageResponse.from(result)));
    }

    // ── Manager dispatch endpoint ─────────────────────────────────────────────

    @GetMapping("/api/v1/manager/dispatch/couriers/available")
    @PreAuthorize(CourierPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<List<CourierDto>>> getAvailableForDispatch() {
        return ResponseEntity.ok(
                ApiResponse.success("Available couriers retrieved", courierService.getAvailableForDispatch())
        );
    }

    @GetMapping("/api/v1/couriers/available")
    @PreAuthorize(CourierPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<List<CourierResponse>>> getAvailableForDispatchAlias() {
        return ResponseEntity.ok(
                ApiResponse.success("Available couriers retrieved", courierService.getAvailableResponsesForDispatch())
        );
    }
}
