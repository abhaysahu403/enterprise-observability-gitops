package com.enterprise.shared.constant;

public final class AppConstants {

    // MDC keys used by every service's logging filter for structured logs
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER = "user";
    public static final String MDC_API = "api";

    // HTTP Header carrying the correlation / request id across service hops
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_AUTH_USER = "X-Auth-User";
    public static final String HEADER_AUTH_ROLES = "X-Auth-Roles";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // Redis cache names
    public static final String CACHE_EMPLOYEE = "employeeCache";
    public static final String CACHE_DASHBOARD = "dashboardCache";
    public static final String CACHE_TOKEN_BLACKLIST = "tokenBlacklist";

    private AppConstants() {
    }
}
