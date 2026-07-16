package com.ivedi.ivedikuryemanagement.commission.api;

import java.math.BigDecimal;

public record EarningsSummaryDto(
        long totalDeliveries,
        BigDecimal totalGross,
        BigDecimal totalNet,
        long pendingCount,
        BigDecimal pendingAmount,
        long paidCount,
        BigDecimal paidAmount
) {}
