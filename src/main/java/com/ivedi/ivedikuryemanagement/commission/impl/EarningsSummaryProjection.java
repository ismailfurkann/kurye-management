package com.ivedi.ivedikuryemanagement.commission.impl;

import java.math.BigDecimal;

public interface EarningsSummaryProjection {
    Long getTotalCount();
    BigDecimal getGrossTotal();
    BigDecimal getNetTotal();
    Long getPendingCount();
    BigDecimal getPendingTotal();
    Long getPaidCount();
    BigDecimal getPaidTotal();
}
