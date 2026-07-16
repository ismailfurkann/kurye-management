package com.ivedi.ivedikuryemanagement.report.impl;

import java.math.BigDecimal;

public interface StatusAggregateProjection {
    String getStatus();
    Long getCnt();
    BigDecimal getTotalFee();
}
