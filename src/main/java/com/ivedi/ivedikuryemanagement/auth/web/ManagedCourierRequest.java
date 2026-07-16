package com.ivedi.ivedikuryemanagement.auth.web;

import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ManagedCourierRequest(
        @NotNull VehicleType vehicleType,
        String plateNumber,
        UUID regionId
) {
}
