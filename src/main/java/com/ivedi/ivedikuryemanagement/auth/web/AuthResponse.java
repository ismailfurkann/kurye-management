package com.ivedi.ivedikuryemanagement.auth.web;

import com.ivedi.ivedikuryemanagement.auth.api.UserDto;
import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword,
        UserDto user
) {}
