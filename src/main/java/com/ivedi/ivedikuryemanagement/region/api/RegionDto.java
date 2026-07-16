package com.ivedi.ivedikuryemanagement.region.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegionDto(
        UUID id,
        String name,
        String city,
        String district,
        String description,
        RegionStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
