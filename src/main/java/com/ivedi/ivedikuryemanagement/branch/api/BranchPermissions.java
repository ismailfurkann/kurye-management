package com.ivedi.ivedikuryemanagement.branch.api;

public final class BranchPermissions {

    private BranchPermissions() {}

    /** Create, update, or change status of a branch. */
    public static final String ADMIN_ONLY =
            "hasRole('PLATFORM_ADMIN')";

    /** Read any branch — no mutation. */
    public static final String ADMIN_OR_MANAGER =
            "hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER')";

    /** Restaurant owner reading own restaurant's branches. */
    public static final String OWNER_ONLY =
            "hasRole('RESTAURANT_OWNER')";
}
