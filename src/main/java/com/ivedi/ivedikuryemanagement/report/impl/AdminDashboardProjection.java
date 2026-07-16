package com.ivedi.ivedikuryemanagement.report.impl;

import java.math.BigDecimal;

public interface AdminDashboardProjection {
    Long getTotalRestaurants();
    Long getTotalBranches();
    Long getTotalCouriers();
    Long getActiveCouriers();
    Long getPendingApprovalCouriers();
    Long getTodayDeliveries();
    BigDecimal getTotalCommission();
    Long getFailedDeliveries();
}
