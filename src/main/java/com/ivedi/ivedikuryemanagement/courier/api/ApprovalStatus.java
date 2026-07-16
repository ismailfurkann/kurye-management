package com.ivedi.ivedikuryemanagement.courier.api;

public enum ApprovalStatus {
    /** Profile submitted, waiting for admin review. */
    PENDING,
    /** Cleared to operate. */
    APPROVED,
    /** Application denied — can be re-evaluated by admin. */
    REJECTED,
    /** Access revoked — cannot self-reactivate. */
    BLOCKED
}
