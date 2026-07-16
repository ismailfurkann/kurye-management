package com.ivedi.ivedikuryemanagement.delivery.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelDeliveryOrderRequest(

        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 500)
        String reason
) {}
