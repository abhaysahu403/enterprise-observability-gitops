package com.enterprise.shared.constant;

/**
 * Central definition of roles used across the enterprise platform.
 * Keeping these in the shared library guarantees every service agrees
 * on the exact role names used in JWT claims and @PreAuthorize checks.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String HR = "HR";
    public static final String EMPLOYEE = "EMPLOYEE";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_HR = "ROLE_HR";
    public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";

    private Roles() {
    }
}
