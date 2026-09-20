package com.invoiceguard.exception;

/** Thrown when a caller (user, API key, or IP) exceeds a configured rate limit. */
public class RateLimitExceededException extends ApplicationException {

    public RateLimitExceededException(String message) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message);
    }
}
