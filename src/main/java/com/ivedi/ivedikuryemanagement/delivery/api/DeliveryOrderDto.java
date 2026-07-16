package com.ivedi.ivedikuryemanagement.delivery.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryOrderDto(
        UUID id,
        String orderCode,

        UUID branchId,
        String branchName,
        String restaurantName,

        String customerName,
        String customerPhone,
        String customerAddressText,
        BigDecimal customerLatitude,
        BigDecimal customerLongitude,

        BigDecimal orderAmount,
        BigDecimal deliveryFee,
        PaymentCollectionType paymentCollectionType,
        String deliveryNote,

        DeliveryOrderStatus status,
        OrderPriority priority,

        UUID createdByUserId,

        OffsetDateTime submittedAt,
        OffsetDateTime assignedAt,
        OffsetDateTime pickedUpAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime failedAt,
        String cancellationReason,
        String failureReason,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
