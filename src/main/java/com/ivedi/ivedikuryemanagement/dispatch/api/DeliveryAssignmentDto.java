package com.ivedi.ivedikuryemanagement.dispatch.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryAssignmentDto(
        UUID id,
        UUID deliveryOrderId,
        String orderCode,
        UUID courierId,
        String courierFullName,
        AssignmentStatus status,
        UUID assignedBy,
        OffsetDateTime assignedAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime pickedUpAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime failedAt,
        OffsetDateTime cancelledAt,
        String failureReason,
        String cancellationReason
) {}
