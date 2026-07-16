package com.ivedi.ivedikuryemanagement.commission.api;

import java.math.BigDecimal;

public record CommissionSummaryResponse(
        long totalDeliveries,
        BigDecimal pendingAmount,
        BigDecimal paidAmount,
        BigDecimal totalAmount,
        BigDecimal currentPeriodAmount
) {
}
