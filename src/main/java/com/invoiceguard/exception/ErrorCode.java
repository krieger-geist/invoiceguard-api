package com.invoiceguard.exception;

import org.springframework.http.HttpStatus;

/**
 * Canonical machine-readable error codes returned in {@code ErrorResponse.code}.
 * New modules should add codes here rather than inventing ad-hoc strings, so the
 * set of possible API error codes stays centrally documented (see Swagger config).
 */
public enum ErrorCode {

    // Generic / validation
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT),

    // Auth / security
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(HttpStatus.LOCKED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    TENANT_MISMATCH(HttpStatus.FORBIDDEN),
    INSUFFICIENT_SCOPE(HttpStatus.FORBIDDEN),
    ORGANIZATION_CONTEXT_REQUIRED(HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),

    // Domain specific
    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND),
    VENDOR_SUSPENDED(HttpStatus.UNPROCESSABLE_ENTITY),
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVOICE_LOCKED_FOR_ANALYSIS(HttpStatus.CONFLICT),
    INVOICE_NOT_EDITABLE(HttpStatus.CONFLICT),
    DUPLICATE_INVOICE_DETECTED(HttpStatus.CONFLICT),
    APPROVAL_NOT_ALLOWED(HttpStatus.FORBIDDEN),
    SELF_APPROVAL_NOT_ALLOWED(HttpStatus.FORBIDDEN),

    // Server side
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
