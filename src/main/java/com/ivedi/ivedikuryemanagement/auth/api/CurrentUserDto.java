package com.ivedi.ivedikuryemanagement.auth.api;

import java.util.List;
import java.util.UUID;

public record CurrentUserDto(
        UUID id,
        String fullName,
        String email,
        String phone,
        UserRole role,
        UUID restaurantId,
        List<UUID> branchIds,
        UUID courierId
) {}
