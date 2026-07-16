package com.ivedi.ivedikuryemanagement.restaurant.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RestaurantDto(
        UUID id,
        String name,
        String legalName,
        String taxNumber,
        String phone,
        String email,
        UUID ownerId,
        String ownerFullName,
        String ownerEmail,
        RestaurantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
