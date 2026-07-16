package com.ivedi.ivedikuryemanagement.dispatch.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
import com.ivedi.ivedikuryemanagement.dispatch.api.DispatchPermissions;
import com.ivedi.ivedikuryemanagement.dispatch.api.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class DispatchController {

    private final DispatchService dispatchService;

    @PostMapping("/api/v1/orders/{orderId}/assign/{courierId}")
    @PreAuthorize(DispatchPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<AssignmentResponse>> assignAlias(
            @PathVariable UUID orderId,
            @PathVariable UUID courierId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order assigned to courier",
                        dispatchService.assignResponse(orderId, courierId)));
    }

    @PostMapping("/api/v1/assignments/{assignmentId}/reassign")
    @PreAuthorize(DispatchPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<AssignmentResponse>> reassignAlias(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignmentReassignRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Order reassigned",
                dispatchService.reassignByAssignmentIdResponse(assignmentId, request.courierId(), request.reason())));
    }

    @PostMapping("/api/v1/manager/dispatch/orders/{orderId}/assign/{courierId}")
    @PreAuthorize(DispatchPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> assign(
            @PathVariable UUID orderId,
            @PathVariable UUID courierId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order assigned to courier",
                        dispatchService.assign(orderId, courierId)));
    }

    @PostMapping("/api/v1/manager/dispatch/orders/{orderId}/reassign")
    @PreAuthorize(DispatchPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> reassign(
            @PathVariable UUID orderId,
            @Valid @RequestBody ReassignRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Order reassigned",
                dispatchService.reassign(orderId, request.newCourierId(), request.reason())));
    }

    @GetMapping("/api/v1/manager/dispatch/orders/{orderId}/assignment")
    @PreAuthorize(DispatchPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> getActiveAssignment(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Active assignment retrieved",
                dispatchService.getActiveAssignment(orderId)));
    }

    @GetMapping("/api/v1/manager/dispatch/orders/{orderId}/assignment-history")
    @PreAuthorize(DispatchPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentDto>>> getAssignmentHistory(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Assignment history retrieved",
                dispatchService.getAssignmentHistory(orderId)));
    }
}
