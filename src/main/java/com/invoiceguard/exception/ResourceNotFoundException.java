package com.invoiceguard.exception;

/** Thrown when a requested resource does not exist (or is invisible to the caller's tenant). */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static ResourceNotFoundException of(String entityName, Object id) {
        return new ResourceNotFoundException("%s not found with id: %s".formatted(entityName, id));
    }
}
