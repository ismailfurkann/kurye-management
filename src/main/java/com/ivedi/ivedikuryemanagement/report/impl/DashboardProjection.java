package com.ivedi.ivedikuryemanagement.report.impl;

import java.math.BigDecimal;

public interface DashboardProjection {
    Long getTodayOrders();
    Long getReadyForDispatch();
    Long getActiveDeliveries();
    Long getDelivered();
    Long getFailed();
    Long getCancelled();
    Long getActiveCouriers();
    BigDecimal getAverageDeliveryMinutes();
}
