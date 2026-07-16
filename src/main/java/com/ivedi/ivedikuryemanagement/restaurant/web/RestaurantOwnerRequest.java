package com.ivedi.ivedikuryemanagement.restaurant.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RestaurantOwnerRequest(

        @NotNull(message = "New owner ID is required")
        UUID ownerId
) {}
