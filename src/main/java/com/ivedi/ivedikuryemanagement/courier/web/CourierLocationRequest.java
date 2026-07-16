package com.ivedi.ivedikuryemanagement.courier.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CourierLocationRequest(

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0000000",  message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0000000",   message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0000000", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0000000",  message = "Longitude must be between -180 and 180")
        BigDecimal longitude
) {}
