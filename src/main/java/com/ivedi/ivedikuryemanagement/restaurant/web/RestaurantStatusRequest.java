package com.ivedi.ivedikuryemanagement.restaurant.web;

import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantStatus;
import jakarta.validation.constraints.NotNull;

public record RestaurantStatusRequest(

        @NotNull(message = "Status is required")
        RestaurantStatus status
) {}
