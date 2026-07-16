package com.ivedi.ivedikuryemanagement.branch.web;

import com.ivedi.ivedikuryemanagement.branch.api.BranchStatus;
import jakarta.validation.constraints.NotNull;

public record BranchStatusRequest(

        @NotNull(message = "Status is required")
        BranchStatus status
) {}
