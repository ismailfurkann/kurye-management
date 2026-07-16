package com.ivedi.ivedikuryemanagement.report.api;

import java.math.BigDecimal;

public record DeliveryStatsDto(
        long total,
        long created,
        long readyForDispatch,
        long assigned,
        long pickedUp,
        long delivered,
        long cancelled,
        long failed,
        BigDecimal totalDeliveryFeeCollected,
        double deliverySuccessRate
) {}
