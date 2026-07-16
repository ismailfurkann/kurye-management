package com.ivedi.ivedikuryemanagement.delivery.api;

public final class DeliveryOrderPermissions {

    private DeliveryOrderPermissions() {}

    /** Create, edit, submit for dispatch, or cancel own branch orders. */
    public static final String RESTAURANT =
            "hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF')";

    /** Restaurant roles + platform managers/admins — read & cancel. */
    public static final String RESTAURANT_OR_MANAGER =
            "hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM_MANAGER', 'PLATFORM_ADMIN')";

    /** Read all orders, manage dispatch queue. */
    public static final String MANAGER_OR_ADMIN =
            "hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')";

    /** Full admin access including forced cancel / mark-failed. */
    public static final String ADMIN_ONLY =
            "hasRole('PLATFORM_ADMIN')";
}
