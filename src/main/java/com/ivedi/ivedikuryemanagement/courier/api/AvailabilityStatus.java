package com.ivedi.ivedikuryemanagement.courier.api;

public enum AvailabilityStatus {
    /** Ready to accept a new assignment. */
    AVAILABLE,
    /** Currently delivering — set by the system, not the courier. */
    BUSY,
    /** Logged out or disconnected. */
    OFFLINE,
    /** Taking a break, will return soon. */
    ON_BREAK
}
