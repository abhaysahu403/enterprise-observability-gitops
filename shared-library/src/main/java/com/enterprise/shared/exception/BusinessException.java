package com.enterprise.shared.exception;

/**
 * Thrown when a business rule is violated (e.g. leave balance exceeded,
 * duplicate email on registration, invalid state transition). Mapped to HTTP 400/409.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_RULE_VIOLATION";
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
