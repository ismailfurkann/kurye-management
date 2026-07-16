package com.ivedi.ivedikuryemanagement.branch.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchDto(
        UUID id,

        UUID restaurantId,
        String restaurantName,

        UUID regionId,
        String regionName,
        String regionCity,
        String regionDistrict,

        String name,
        String phone,
        String addressText,
        BigDecimal latitude,
        BigDecimal longitude,

        BranchStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
