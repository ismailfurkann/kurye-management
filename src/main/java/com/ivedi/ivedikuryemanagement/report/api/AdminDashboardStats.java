package com.ivedi.ivedikuryemanagement.report.api;

import java.math.BigDecimal;

public record AdminDashboardStats(
        long totalRestaurants,
        long totalBranches,
        long totalCouriers,
        long activeCouriers,
        long pendingApprovalCouriers,
        long todayDeliveries,
        BigDecimal totalCommission,
        long failedDeliveries
) {
}
