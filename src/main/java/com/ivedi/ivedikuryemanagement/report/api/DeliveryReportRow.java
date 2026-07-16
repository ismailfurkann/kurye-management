package com.ivedi.ivedikuryemanagement.report.api;

import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryReportRow(
        UUID orderId,
        String orderCode,
        UUID restaurantId,
        String restaurantName,
        UUID branchId,
        String branchName,
        UUID courierId,
        String courierName,
        DeliveryOrderStatus status,
        BigDecimal orderAmount,
        BigDecimal deliveryFee,
        BigDecimal commissionAmount,
        OffsetDateTime createdAt,
        OffsetDateTime deliveredAt,
        BigDecimal deliveryMinutes
) {
}
