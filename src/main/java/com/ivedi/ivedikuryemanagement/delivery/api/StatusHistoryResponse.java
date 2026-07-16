package com.ivedi.ivedikuryemanagement.delivery.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID id,
        UUID orderId,
        DeliveryOrderStatus oldStatus,
        DeliveryOrderStatus newStatus,
        UUID actorUserId,
        String actorName,
        String note,
        OffsetDateTime occurredAt
) {}
