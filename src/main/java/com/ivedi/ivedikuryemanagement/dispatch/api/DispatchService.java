package com.ivedi.ivedikuryemanagement.dispatch.api;

import java.util.List;
import java.util.UUID;

public interface DispatchService {

    /**
     * Assigns a READY_FOR_DISPATCH order to an AVAILABLE+APPROVED courier.
     * Transitions order → ASSIGNED, courier → BUSY.
     * Throws 409 if another assignment exists or optimistic lock conflict.
     */
    DeliveryAssignmentDto assign(UUID orderId, UUID courierId);

    AssignmentResponse assignResponse(UUID orderId, UUID courierId);

    /**
     * Reassigns an ASSIGNED/ACCEPTED order to a different courier.
     * Cancels the existing active assignment, creates a new one.
     * Order stays ASSIGNED. Previous courier's count is decremented.
     */
    DeliveryAssignmentDto reassign(UUID orderId, UUID newCourierId, String reason);

    AssignmentResponse reassignByAssignmentIdResponse(UUID assignmentId, UUID newCourierId, String reason);

    /**
     * Returns full assignment history for an order.
     */
    List<DeliveryAssignmentDto> getAssignmentHistory(UUID orderId);

    /**
     * Returns the current active assignment for an order, if any.
     */
    DeliveryAssignmentDto getActiveAssignment(UUID orderId);
}
