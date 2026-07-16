package com.ivedi.ivedikuryemanagement.courier.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CourierDto(
        UUID id,

        UUID userId,
        String userFullName,
        String userEmail,
        String userPhone,

        VehicleType vehicleType,
        AvailabilityStatus availabilityStatus,
        ApprovalStatus approvalStatus,

        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        OffsetDateTime lastLocationAt,

        BigDecimal rating,
        int activeAssignmentCount,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
