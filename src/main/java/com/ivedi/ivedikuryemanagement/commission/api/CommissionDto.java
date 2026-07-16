package com.ivedi.ivedikuryemanagement.commission.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommissionDto(
        UUID id,
        UUID deliveryAssignmentId,
        UUID deliveryOrderId,
        String orderCode,
        UUID courierId,
        String courierFullName,
        BigDecimal grossAmount,
        BigDecimal commissionRate,
        BigDecimal netAmount,
        CommissionStatus status,
        String paymentPeriod,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {}
