package com.ivedi.ivedikuryemanagement.branch.web;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record BranchRequest(

        /** Required on create. Ignored on update — restaurant cannot change after creation. */
        @NotNull(message = "Restaurant ID is required")
        UUID restaurantId,

        @NotNull(message = "Region ID is required")
        UUID regionId,

        @NotBlank(message = "Branch name is required")
        @Size(max = 150, message = "Branch name must not exceed 150 characters")
        String name,

        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
        String phone,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String addressText,

        @DecimalMin(value = "-90.0000000",  message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0000000",   message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0000000", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0000000",  message = "Longitude must be between -180 and 180")
        BigDecimal longitude
) {}
