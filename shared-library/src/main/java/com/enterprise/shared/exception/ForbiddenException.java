package com.enterprise.shared.exception;

/**
 * Thrown when an authenticated user lacks the role/permission required for
 * an operation. Mapped to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
