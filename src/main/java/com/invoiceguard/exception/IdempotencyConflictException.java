package com.invoiceguard.exception;

/** Thrown when an Idempotency-Key is reused with a request body that does not match the original. */
public class IdempotencyConflictException extends ApplicationException {

    public IdempotencyConflictException(String message) {
        super(ErrorCode.IDEMPOTENCY_KEY_REUSED, message);
    }
}
