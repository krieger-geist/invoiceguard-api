package com.invoiceguard.exception;

/** Thrown when a request is structurally valid but violates a domain business rule. */
public class BusinessRuleViolationException extends ApplicationException {

    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
