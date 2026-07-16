package com.ivedi.ivedikuryemanagement.auth.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserManagementResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        UUID restaurantId,
        String restaurantName,
        List<UUID> branchIds,
        List<String> branchNames,
        UUID courierId,
        boolean mustChangePassword,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
