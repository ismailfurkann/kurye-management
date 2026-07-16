package com.ivedi.ivedikuryemanagement.realtime.api;

import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentEvent(
        String type,           // "ASSIGNMENT_CREATED" | "ASSIGNMENT_STATUS_CHANGED"
        UUID assignmentId,
        UUID orderId,
        String orderCode,
        UUID courierId,
        AssignmentStatus status,
        OffsetDateTime occurredAt
) {
    public static AssignmentEvent created(UUID assignmentId, UUID orderId, String orderCode, UUID courierId) {
        return new AssignmentEvent("ASSIGNMENT_CREATED",
                assignmentId, orderId, orderCode, courierId, AssignmentStatus.ASSIGNED, OffsetDateTime.now());
    }

    public static AssignmentEvent statusChanged(UUID assignmentId, UUID orderId, String orderCode,
                                                UUID courierId, AssignmentStatus status) {
        return new AssignmentEvent("ASSIGNMENT_STATUS_CHANGED",
                assignmentId, orderId, orderCode, courierId, status, OffsetDateTime.now());
    }
}
