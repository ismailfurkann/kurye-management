package com.ivedi.ivedikuryemanagement.courier.api;

public final class CourierPermissions {

    private CourierPermissions() {}

    /** Create profile, update availability, update location. */
    public static final String COURIER_ONLY =
            "hasRole('COURIER')";

    /** Read courier list, read individual courier. */
    public static final String ADMIN_OR_MANAGER =
            "hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')";

    /** Change approval status (approve / reject / block). */
    public static final String ADMIN_ONLY =
            "hasRole('PLATFORM_ADMIN')";
}
