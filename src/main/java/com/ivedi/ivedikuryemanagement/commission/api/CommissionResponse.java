package com.ivedi.ivedikuryemanagement.commission.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommissionResponse(
        UUID id,
        UUID courierId,
        String courierName,
        UUID orderId,
        String orderCode,
        BigDecimal amount,
        BigDecimal rate,
        BigDecimal netAmount,
        CommissionStatus status,
        String period,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {
}
