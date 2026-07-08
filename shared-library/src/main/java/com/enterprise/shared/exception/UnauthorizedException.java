package com.enterprise.shared.exception;

/**
 * Thrown for authentication failures (bad credentials, expired/invalid token,
 * blacklisted token). Mapped to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
