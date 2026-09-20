package com.invoiceguard.exception;

/** Thrown when an operation would create a resource that conflicts with an existing unique one. */
public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }

    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
