package com.ivedi.ivedikuryemanagement.region.web;

import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import jakarta.validation.constraints.NotNull;

public record RegionStatusRequest(

        @NotNull(message = "Status is required")
        RegionStatus status
) {}
