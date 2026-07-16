package com.ivedi.ivedikuryemanagement.restaurant.api;

public enum RestaurantStatus {
    /** Waiting for platform admin approval. */
    PENDING,
    /** Fully operational. */
    ACTIVE,
    /** Temporarily deactivated by owner or admin. */
    PASSIVE,
    /** Blocked by platform admin — cannot be self-reactivated. */
    SUSPENDED
}
