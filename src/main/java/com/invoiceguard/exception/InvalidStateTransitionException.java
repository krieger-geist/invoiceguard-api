package com.invoiceguard.exception;

/** Thrown when an entity's status cannot legally move to the requested target status. */
public class InvalidStateTransitionException extends ApplicationException {

    public InvalidStateTransitionException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }

    public static InvalidStateTransitionException of(String entityName, Object from, Object to) {
        return new InvalidStateTransitionException(
                "%s cannot transition from %s to %s".formatted(entityName, from, to));
    }
}
