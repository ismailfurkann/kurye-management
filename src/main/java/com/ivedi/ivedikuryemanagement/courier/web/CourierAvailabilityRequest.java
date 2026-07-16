package com.ivedi.ivedikuryemanagement.courier.web;

import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record CourierAvailabilityRequest(

        @NotNull(message = "Availability status is required")
        AvailabilityStatus status
) {}
