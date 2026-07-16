package com.ivedi.ivedikuryemanagement.delivery.api;

import com.ivedi.ivedikuryemanagement.delivery.web.CancelDeliveryOrderRequest;
import com.ivedi.ivedikuryemanagement.delivery.web.CreateDeliveryOrderRequest;
import com.ivedi.ivedikuryemanagement.delivery.web.UpdateDeliveryOrderRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface DeliveryOrderService {

    // ── Restaurant endpoints ───────────────────────────────────────────────────

    /**
     * Creates a new delivery order for a branch owned by the current user.
     * Order starts in CREATED status. A history entry is written.
     */
    DeliveryOrderDto create(CreateDeliveryOrderRequest request);

    OrderResponse createResponse(CreateDeliveryOrderRequest request);

    /**
     * Updates a delivery order. Only allowed in CREATED status.
     * Verifies the order belongs to the current user's restaurant.
     */
    DeliveryOrderDto update(UUID id, UpdateDeliveryOrderRequest request);

    OrderResponse updateResponse(UUID id, UpdateDeliveryOrderRequest request);

    /**
     * Returns a single order. Restaurant users can only access their own branch orders.
     */
    DeliveryOrderDto getById(UUID id);

    OrderResponse getResponseById(UUID id);

    /**
     * Paginated list of orders for the current user's restaurant branches.
     */
    Page<DeliveryOrderDto> getMyOrders(DeliveryOrderSearchDto searchDto);

    Page<OrderResponse> getMyOrderResponses(DeliveryOrderSearchDto searchDto);

    /**
     * Transitions order from CREATED → READY_FOR_DISPATCH.
     * Verifies ownership. Sets submittedAt timestamp.
     */
    DeliveryOrderDto submitForDispatch(UUID id);

    OrderResponse submitForDispatchResponse(UUID id);

    /**
     * Cancels an order. Restaurant can cancel in CREATED or READY_FOR_DISPATCH.
     * Manager can cancel up to ASSIGNED (with mandatory reason).
     */
    DeliveryOrderDto cancel(UUID id, CancelDeliveryOrderRequest request);

    OrderResponse cancelResponse(UUID id, CancelDeliveryOrderRequest request);

    // ── Manager / Admin endpoints ─────────────────────────────────────────────

    /**
     * Admin/manager: paginated full search across all orders.
     */
    Page<DeliveryOrderDto> search(DeliveryOrderSearchDto searchDto);

    Page<OrderResponse> searchResponses(DeliveryOrderSearchDto searchDto);

    /**
     * Admin/manager: returns all READY_FOR_DISPATCH orders.
     * Sorted: URGENT → HIGH → NORMAL, then oldest submittedAt first.
     */
    List<DeliveryOrderDto> getReadyForDispatch();

    List<OrderResponse> getReadyResponsesForDispatch();

    /**
     * Admin/manager: returns all ASSIGNED and PICKED_UP orders.
     * Sorted by assignedAt ascending (oldest active first).
     */
    List<DeliveryOrderDto> getActiveOrders();

    /**
     * Returns full status history for an order.
     */
    List<DeliveryStatusHistoryDto> getStatusHistory(UUID id);

    List<StatusHistoryResponse> getStatusHistoryResponses(UUID id);

    // ── Internal — called by dispatch and assignment modules ──────────────────

    /**
     * Performs a validated status transition on behalf of an actor.
     * Writes a history record. Used by dispatch and assignment modules.
     * Handles setting timestamp fields (assignedAt, pickedUpAt, deliveredAt, etc.).
     */
    void transitionStatus(UUID orderId, DeliveryOrderStatus newStatus, UUID actorUserId, String note);
}
