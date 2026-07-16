package com.ivedi.ivedikuryemanagement.restaurant.api;

public final class RestaurantPermissions {

    private RestaurantPermissions() {}

    /** Create, update, change status, or reassign owner. */
    public static final String ADMIN_ONLY =
            "hasRole('PLATFORM_ADMIN')";

    /** Read any restaurant — no mutation. */
    public static final String ADMIN_OR_MANAGER =
            "hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')";

    /** Owners reading their own restaurants. */
    public static final String OWNER_ONLY =
            "hasRole('RESTAURANT_OWNER')";
}
