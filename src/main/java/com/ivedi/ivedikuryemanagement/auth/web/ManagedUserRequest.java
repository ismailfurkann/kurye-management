package com.ivedi.ivedikuryemanagement.auth.web;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ManagedUserRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @NotNull UserRole role,
        UUID restaurantId,
        List<UUID> branchIds,
        @NotBlank @Size(min = 8, max = 72) String temporaryPassword,
        @Valid ManagedCourierRequest courier
) {
}
