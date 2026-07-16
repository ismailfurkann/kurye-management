package com.ivedi.ivedikuryemanagement.delivery.api;

import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderCode,
        UUID branchId,
        String branchName,
        UUID restaurantId,
        String restaurantName,
        String customerName,
        String customerPhone,
        String customerAddress,
        BigDecimal customerLatitude,
        BigDecimal customerLongitude,
        BigDecimal orderAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        PaymentCollectionType paymentCollectionType,
        OrderPriority priority,
        String notes,
        String status,
        List<StatusHistoryResponse> statusHistory,
        AssignmentResponse activeAssignment,
        OffsetDateTime submittedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
