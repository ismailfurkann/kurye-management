package com.ivedi.ivedikuryemanagement.dispatch.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentService;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
import com.ivedi.ivedikuryemanagement.dispatch.api.DispatchPermissions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping("/api/v1/assignments/me")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getMyAssignmentsAlias() {
        return ResponseEntity.ok(ApiResponse.success("Active assignments retrieved",
                assignmentService.getMyActiveAssignmentResponses()));
    }

    @GetMapping("/api/v1/assignments/active")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getActiveAssignmentsAlias() {
        return ResponseEntity.ok(ApiResponse.success("Active assignments retrieved",
                assignmentService.getMyActiveAssignmentResponses()));
    }

    @PostMapping("/api/v1/assignments/{id}/accept")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<AssignmentResponse>> acceptAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Assignment accepted",
                assignmentService.acceptResponse(id)));
    }

    @PostMapping("/api/v1/assignments/{id}/reject")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<AssignmentResponse>> rejectAlias(
            @PathVariable UUID id,
            @Valid @RequestBody RejectionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Assignment rejected",
                assignmentService.rejectResponse(id, request.reason())));
    }

    @PostMapping("/api/v1/assignments/{id}/pickup")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<AssignmentResponse>> pickupAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Pickup confirmed",
                assignmentService.confirmPickupResponse(id)));
    }

    @PostMapping("/api/v1/assignments/{id}/deliver")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<AssignmentResponse>> deliverAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Delivery confirmed",
                assignmentService.confirmDeliveryResponse(id)));
    }

    @PostMapping("/api/v1/assignments/{id}/fail")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<AssignmentResponse>> failAlias(
            @PathVariable UUID id,
            @Valid @RequestBody FailureReportRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Failure reported",
                assignmentService.reportFailureResponse(id, request.reason())));
    }

    @GetMapping("/api/v1/courier/assignments")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentDto>>> getMyActiveAssignments() {
        return ResponseEntity.ok(ApiResponse.success("Active assignments retrieved",
                assignmentService.getMyActiveAssignments()));
    }

    @GetMapping("/api/v1/courier/assignments/{id}")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> getMyAssignment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Assignment retrieved",
                assignmentService.getMyAssignment(id)));
    }

    @PatchMapping("/api/v1/courier/assignments/{id}/accept")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Assignment accepted",
                assignmentService.accept(id)));
    }

    @PatchMapping("/api/v1/courier/assignments/{id}/reject")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Assignment rejected",
                assignmentService.reject(id, request.reason())));
    }

    @PatchMapping("/api/v1/courier/assignments/{id}/picked-up")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> confirmPickup(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Pickup confirmed",
                assignmentService.confirmPickup(id)));
    }

    @PatchMapping("/api/v1/courier/assignments/{id}/delivered")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> confirmDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Delivery confirmed",
                assignmentService.confirmDelivery(id)));
    }

    @PatchMapping("/api/v1/courier/assignments/{id}/failed")
    @PreAuthorize(DispatchPermissions.COURIER_ONLY)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> reportFailure(
            @PathVariable UUID id,
            @Valid @RequestBody FailureReportRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Failure reported",
                assignmentService.reportFailure(id, request.reason())));
    }
}
