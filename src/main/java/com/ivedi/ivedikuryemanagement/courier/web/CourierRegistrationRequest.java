package com.ivedi.ivedikuryemanagement.courier.web;

import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import jakarta.validation.constraints.NotNull;

public record CourierRegistrationRequest(

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType
) {}
