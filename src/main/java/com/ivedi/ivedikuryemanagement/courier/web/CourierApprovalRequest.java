package com.ivedi.ivedikuryemanagement.courier.web;

import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record CourierApprovalRequest(

        @NotNull(message = "Approval status is required")
        ApprovalStatus status
) {}
