package com.invoiceguard.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard envelope wrapping every error API response.
 *
 * <pre>
 * {
 *   "success": false,
 *   "code": "INVOICE_NOT_FOUND",
 *   "message": "Invoice was not found",
 *   "fieldErrors": [],
 *   "timestamp": "2026-08-02T10:00:00Z",
 *   "path": "/api/v1/invoices/10",
 *   "correlationId": "..."
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        List<FieldErrorItem> fieldErrors,
        Instant timestamp,
        String path,
        String correlationId) {

    public static ErrorResponse of(String code, String message, String path, String correlationId) {
        return new ErrorResponse(false, code, message, List.of(), Instant.now(), path, correlationId);
    }

    public static ErrorResponse ofValidation(
            String code, String message, List<FieldErrorItem> fieldErrors, String path, String correlationId) {
        return new ErrorResponse(false, code, message, fieldErrors, Instant.now(), path, correlationId);
    }

    /** A single field-level validation failure. */
    public record FieldErrorItem(String field, String message, Object rejectedValue) {}
}
