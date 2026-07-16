package com.ivedi.ivedikuryemanagement.report.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface DeliveryReportProjection {
    UUID getOrderId();
    String getOrderCode();
    UUID getRestaurantId();
    String getRestaurantName();
    UUID getBranchId();
    String getBranchName();
    UUID getCourierId();
    String getCourierName();
    String getStatus();
    BigDecimal getOrderAmount();
    BigDecimal getDeliveryFee();
    BigDecimal getCommissionAmount();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getDeliveredAt();
    BigDecimal getDeliveryMinutes();
}
