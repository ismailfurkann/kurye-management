package com.ivedi.ivedikuryemanagement.dispatch.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailureReportRequest(

        @NotBlank(message = "Failure reason is required")
        @Size(max = 500)
        String reason
) {}
