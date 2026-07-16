package com.ivedi.ivedikuryemanagement.restaurant.web;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record RestaurantRequest(

        @NotBlank(message = "Restaurant name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 200, message = "Legal name must not exceed 200 characters")
        String legalName,

        @Pattern(regexp = "^[0-9]{10}$", message = "Tax number must be exactly 10 digits")
        String taxNumber,

        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
        String phone,

        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotNull(message = "Owner ID is required")
        UUID ownerId
) {}
