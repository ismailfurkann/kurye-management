package com.ivedi.ivedikuryemanagement.commission.impl;

import java.math.BigDecimal;

public interface CommissionSummaryProjection {
    Long getTotalDeliveries();
    BigDecimal getPendingAmount();
    BigDecimal getPaidAmount();
    BigDecimal getTotalAmount();
    BigDecimal getCurrentPeriodAmount();
}
