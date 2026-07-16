package com.ivedi.ivedikuryemanagement.region.api;

public final class RegionPermissions {

    private RegionPermissions() {}

    /** Create, update, or change status of a region. */
    public static final String ADMIN_ONLY =
            "hasRole('PLATFORM_ADMIN')";

    /** Read any region data. */
    public static final String AUTHENTICATED =
            "isAuthenticated()";
}
