package com.ivedi.ivedikuryemanagement.delivery.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.delivery.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;

    @GetMapping("/api/v1/orders")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT_OR_MANAGER)
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) OrderPriority priority,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<OrderResponse> result = deliveryOrderService.searchResponses(
                new DeliveryOrderSearchDto(
                        branchId,
                        restaurantId,
                        parseEntityStatus(status),
                        status,
                        search,
                        priority,
                        startDate,
                        endDate,
                        page,
                        size
                )
        );
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/orders/ready-for-dispatch")
    @PreAuthorize(DeliveryOrderPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getReadyForDispatchAlias() {
        return ResponseEntity.ok(ApiResponse.success("Ready-for-dispatch orders retrieved",
                deliveryOrderService.getReadyResponsesForDispatch()));
    }

    @PostMapping("/api/v1/orders")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT)
    public ResponseEntity<ApiResponse<OrderResponse>> createAlias(
            @Valid @RequestBody CreateDeliveryOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created", deliveryOrderService.createResponse(request)));
    }

    @GetMapping("/api/v1/orders/{id}")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT_OR_MANAGER)
    public ResponseEntity<ApiResponse<OrderResponse>> getByIdAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Order retrieved", deliveryOrderService.getResponseById(id)));
    }

    @PutMapping("/api/v1/orders/{id}")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT)
    public ResponseEntity<ApiResponse<OrderResponse>> updateAlias(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Order updated", deliveryOrderService.updateResponse(id, request)));
    }

    @PostMapping("/api/v1/orders/{id}/submit")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT)
    public ResponseEntity<ApiResponse<OrderResponse>> submitAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Order submitted for dispatch",
                deliveryOrderService.submitForDispatchResponse(id)));
    }

    @PostMapping("/api/v1/orders/{id}/cancel")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT_OR_MANAGER)
    public ResponseEntity<ApiResponse<OrderResponse>> cancelAlias(
            @PathVariable UUID id,
            @Valid @RequestBody CancelDeliveryOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled",
                deliveryOrderService.cancelResponse(id, request)));
    }

    @GetMapping("/api/v1/orders/{id}/history")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT_OR_MANAGER)
    public ResponseEntity<ApiResponse<List<StatusHistoryResponse>>> getStatusHistoryAlias(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success("Status history retrieved",
                deliveryOrderService.getStatusHistoryResponses(id)));
    }

    // ── Restaurant: create & manage own orders ────────────────────────────────

    @PostMapping("/api/v1/delivery-orders")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT)
    public ResponseEntity<ApiResponse<DeliveryOrderDto>> create(
            @Valid @RequestBody CreateDeliveryOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created", deliveryOrderService.create(request)));
    }

    @PatchMapping("/api/v1/delivery-orders/{id}")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT)
    public ResponseEntity<ApiResponse<DeliveryOrderDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryOrderRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Order updated", deliveryOrderService.update(id, request))
        );
    }

    @GetMapping("/api/v1/delivery-orders/{id}")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT_OR_MANAGER)
    public ResponseEntity<ApiResponse<DeliveryOrderDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Order retrieved", deliveryOrderService.getById(id))
        );
    }

    @GetMapping("/api/v1/delivery-orders/my-branch")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT)
    public ResponseEntity<ApiResponse<PageResponse<DeliveryOrderDto>>> getMyOrders(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) DeliveryOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<DeliveryOrderDto> result = deliveryOrderService.getMyOrders(
                new DeliveryOrderSearchDto(branchId, null, status, dateFrom, dateTo, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", PageResponse.from(result)));
    }

    @PatchMapping("/api/v1/delivery-orders/{id}/submit-for-dispatch")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT)
    public ResponseEntity<ApiResponse<DeliveryOrderDto>> submitForDispatch(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Order submitted for dispatch", deliveryOrderService.submitForDispatch(id))
        );
    }

    @PatchMapping("/api/v1/delivery-orders/{id}/cancel")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT_OR_MANAGER)
    public ResponseEntity<ApiResponse<DeliveryOrderDto>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelDeliveryOrderRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Order cancelled", deliveryOrderService.cancel(id, request))
        );
    }

    // ── Manager / Admin: dispatch panel ──────────────────────────────────────

    @GetMapping("/api/v1/manager/dispatch/orders/ready")
    @PreAuthorize(DeliveryOrderPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<List<DeliveryOrderDto>>> getReadyForDispatch() {
        return ResponseEntity.ok(
                ApiResponse.success("Ready-for-dispatch orders retrieved",
                        deliveryOrderService.getReadyForDispatch())
        );
    }

    @GetMapping("/api/v1/manager/dispatch/orders/active")
    @PreAuthorize(DeliveryOrderPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<List<DeliveryOrderDto>>> getActiveOrders() {
        return ResponseEntity.ok(
                ApiResponse.success("Active orders retrieved", deliveryOrderService.getActiveOrders())
        );
    }

    @GetMapping("/api/v1/admin/delivery-orders")
    @PreAuthorize(DeliveryOrderPermissions.MANAGER_OR_ADMIN)
    public ResponseEntity<ApiResponse<PageResponse<DeliveryOrderDto>>> search(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) DeliveryOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<DeliveryOrderDto> result = deliveryOrderService.search(
                new DeliveryOrderSearchDto(branchId, restaurantId, status, dateFrom, dateTo, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", PageResponse.from(result)));
    }

    // ── Status history ────────────────────────────────────────────────────────

    @GetMapping("/api/v1/delivery-orders/{id}/history")
    @PreAuthorize(DeliveryOrderPermissions.RESTAURANT_OR_MANAGER)
    public ResponseEntity<ApiResponse<List<DeliveryStatusHistoryDto>>> getStatusHistory(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Status history retrieved", deliveryOrderService.getStatusHistory(id))
        );
    }

    private DeliveryOrderStatus parseEntityStatus(String status) {
        if (status == null || status.isBlank() || "ACCEPTED".equalsIgnoreCase(status)) {
            return null;
        }
        return DeliveryOrderStatus.valueOf(status);
    }
}
