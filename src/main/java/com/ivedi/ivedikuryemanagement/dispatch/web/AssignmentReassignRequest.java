package com.ivedi.ivedikuryemanagement.dispatch.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignmentReassignRequest(
        @NotNull UUID courierId,
        String reason
) {
}
