package com.ivedi.ivedikuryemanagement.courier.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CourierResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        VehicleType vehicleType,
        String plateNumber,
        AvailabilityStatus availability,
        ApprovalStatus approvalStatus,
        BigDecimal rating,
        long activeAssignmentCount,
        long totalDeliveries,
        UUID regionId,
        String regionName,
        BigDecimal lastLocationLat,
        BigDecimal lastLocationLng,
        OffsetDateTime lastLocationAt,
        String avatarUrl,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt
) {}
