package com.ivedi.ivedikuryemanagement.dispatch.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReassignRequest(

        @NotNull(message = "New courier ID is required")
        UUID newCourierId,

        @Size(max = 500)
        String reason
) {}
