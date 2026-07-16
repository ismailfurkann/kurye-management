package com.ivedi.ivedikuryemanagement.report.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CourierPerformanceDto(
        UUID courierId,
        String courierFullName,
        long totalAssignments,
        long delivered,
        long rejected,
        long failed,
        double successRate,
        BigDecimal totalEarnings,
        BigDecimal rating
) {}
