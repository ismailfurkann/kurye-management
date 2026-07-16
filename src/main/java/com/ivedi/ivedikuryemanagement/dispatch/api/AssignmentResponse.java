package com.ivedi.ivedikuryemanagement.dispatch.api;

import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID orderId,
        String orderCode,
        UUID courierId,
        String courierName,
        String courierPhone,
        VehicleType courierVehicleType,
        AssignmentStatus status,
        OffsetDateTime assignedAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime pickedUpAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime failedAt,
        OffsetDateTime cancelledAt,
        String failureReason,
        String restaurantName,
        String branchName,
        String customerAddress,
        BigDecimal orderAmount,
        BigDecimal deliveryFee,
        PaymentCollectionType paymentCollectionType,
        String notes,
        String customerName,
        String customerPhone
) {}
