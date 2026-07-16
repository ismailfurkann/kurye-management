package com.ivedi.ivedikuryemanagement.report.api;

import java.math.BigDecimal;

public record DashboardStats(
        long todayOrders,
        long readyForDispatch,
        long activeDeliveries,
        long delivered,
        long failed,
        long cancelled,
        long activeCouriers,
        BigDecimal averageDeliveryMinutes
) {
}
