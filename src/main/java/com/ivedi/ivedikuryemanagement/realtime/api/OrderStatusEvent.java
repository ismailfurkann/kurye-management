package com.ivedi.ivedikuryemanagement.realtime.api;

import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderStatusEvent(
        String type,           // "ORDER_STATUS_CHANGED"
        UUID orderId,
        String orderCode,
        UUID branchId,
        UUID restaurantId,
        DeliveryOrderStatus oldStatus,
        DeliveryOrderStatus newStatus,
        UUID actorId,
        OffsetDateTime occurredAt
) {
    public static OrderStatusEvent of(UUID orderId, String orderCode,
                                      UUID branchId, UUID restaurantId,
                                      DeliveryOrderStatus oldStatus, DeliveryOrderStatus newStatus,
                                      UUID actorId) {
        return new OrderStatusEvent("ORDER_STATUS_CHANGED", orderId, orderCode,
                branchId, restaurantId, oldStatus, newStatus, actorId, OffsetDateTime.now());
    }
}
