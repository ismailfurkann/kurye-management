package com.ivedi.ivedikuryemanagement.auth.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String fullName,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        OffsetDateTime createdAt
) {}
