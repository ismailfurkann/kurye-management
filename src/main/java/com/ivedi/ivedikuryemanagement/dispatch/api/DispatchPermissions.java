package com.ivedi.ivedikuryemanagement.dispatch.api;

public final class DispatchPermissions {

    private DispatchPermissions() {}

    public static final String MANAGER_OR_ADMIN =
            "hasAnyRole('PLATFORM_MANAGER', 'PLATFORM_ADMIN')";

    public static final String COURIER_ONLY =
            "hasRole('COURIER')";
}
