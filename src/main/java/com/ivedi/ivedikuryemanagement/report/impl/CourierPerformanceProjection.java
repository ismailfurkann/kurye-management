package com.ivedi.ivedikuryemanagement.report.impl;

import java.math.BigDecimal;
import java.util.UUID;

public interface CourierPerformanceProjection {
    UUID getCourierId();
    String getCourierName();
    String getVehicleType();
    BigDecimal getRating();
    Long getTotalAssignments();
    Long getDeliveredCount();
    Long getFailedCount();
    BigDecimal getAvgDeliveryMinutes();
}
