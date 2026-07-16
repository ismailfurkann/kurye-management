package com.ivedi.ivedikuryemanagement.restaurant.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        String name,
        String email,
        String phone,
        RestaurantStatus status,
        UUID ownerUserId,
        String ownerName,
        long branchCount,
        long activeCourierCount,
        long todayOrderCount,
        BigDecimal totalCommission,
        String logoUrl,
        OffsetDateTime createdAt
) {}
