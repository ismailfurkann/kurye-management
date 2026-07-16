package com.ivedi.ivedikuryemanagement.delivery.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryStatusHistoryDto(
        UUID id,
        UUID deliveryOrderId,
        DeliveryOrderStatus oldStatus,
        DeliveryOrderStatus newStatus,
        UUID changedByUserId,
        OffsetDateTime changedAt,
        String note
) {}
