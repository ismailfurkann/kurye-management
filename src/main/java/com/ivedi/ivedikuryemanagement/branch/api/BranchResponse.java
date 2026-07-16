package com.ivedi.ivedikuryemanagement.branch.api;

import java.math.BigDecimal;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        String name,
        UUID restaurantId,
        String restaurantName,
        String address,
        String district,
        String city,
        String phone,
        boolean isActive,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static BranchResponse from(BranchDto branch) {
        return new BranchResponse(
                branch.id(),
                branch.name(),
                branch.restaurantId(),
                branch.restaurantName(),
                branch.addressText(),
                branch.regionDistrict(),
                branch.regionCity(),
                branch.phone(),
                branch.status() == BranchStatus.ACTIVE,
                branch.latitude(),
                branch.longitude()
        );
    }
}
