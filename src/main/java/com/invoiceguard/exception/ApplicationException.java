package com.invoiceguard.exception;

import lombok.Getter;

/**
 * Base class for every checked business exception in the system.
 * Carries an {@link ErrorCode} so the {@link GlobalExceptionHandler} can map
 * it to the correct HTTP status and machine-readable code without any
 * exception-name string matching.
 */
@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
