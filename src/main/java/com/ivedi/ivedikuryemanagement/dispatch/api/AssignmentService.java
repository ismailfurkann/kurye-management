package com.ivedi.ivedikuryemanagement.dispatch.api;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {

    /**
     * Courier accepts the assignment. ASSIGNED → ACCEPTED.
     */
    DeliveryAssignmentDto accept(UUID assignmentId);

    AssignmentResponse acceptResponse(UUID assignmentId);

    /**
     * Courier rejects the assignment. ASSIGNED → REJECTED.
     * Decrements courier's active count. Order goes back to READY_FOR_DISPATCH.
     */
    DeliveryAssignmentDto reject(UUID assignmentId, String reason);

    AssignmentResponse rejectResponse(UUID assignmentId, String reason);

    /**
     * Courier confirms pickup from restaurant. ACCEPTED → PICKED_UP.
     * Transitions delivery order to PICKED_UP.
     */
    DeliveryAssignmentDto confirmPickup(UUID assignmentId);

    AssignmentResponse confirmPickupResponse(UUID assignmentId);

    /**
     * Courier confirms delivery to customer. PICKED_UP → DELIVERED.
     * Transitions delivery order to DELIVERED. Decrements courier active count.
     */
    DeliveryAssignmentDto confirmDelivery(UUID assignmentId);

    AssignmentResponse confirmDeliveryResponse(UUID assignmentId);

    /**
     * Courier reports a failed delivery. PICKED_UP → FAILED.
     * Transitions delivery order to FAILED. Decrements courier active count.
     */
    DeliveryAssignmentDto reportFailure(UUID assignmentId, String reason);

    AssignmentResponse reportFailureResponse(UUID assignmentId, String reason);

    /**
     * Returns the courier's current active assignments.
     */
    List<DeliveryAssignmentDto> getMyActiveAssignments();

    List<AssignmentResponse> getMyActiveAssignmentResponses();

    /**
     * Returns a specific assignment for the current courier.
     */
    DeliveryAssignmentDto getMyAssignment(UUID assignmentId);

    AssignmentResponse getMyAssignmentResponse(UUID assignmentId);
}
