package com.ivedi.ivedikuryemanagement.dispatch.api;

public enum AssignmentStatus {
    /** Manager assigned the order — courier hasn't accepted yet. */
    ASSIGNED,
    /** Courier accepted the assignment. */
    ACCEPTED,
    /** Courier picked up the order from the restaurant. */
    PICKED_UP,
    /** Courier delivered the order to the customer. Terminal — success. */
    DELIVERED,
    /** Courier rejected the assignment. Terminal — reassign required. */
    REJECTED,
    /** Manager cancelled the assignment (e.g., reassign). Terminal. */
    CANCELLED,
    /** Courier reported a delivery failure. Terminal — manager must handle. */
    FAILED;

    public boolean isTerminal() {
        return this == DELIVERED || this == REJECTED || this == CANCELLED || this == FAILED;
    }

    public boolean isActive() {
        return this == ASSIGNED || this == ACCEPTED || this == PICKED_UP;
    }
}
