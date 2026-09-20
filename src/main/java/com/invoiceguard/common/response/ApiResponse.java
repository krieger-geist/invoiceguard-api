package com.invoiceguard.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Standard envelope wrapping every successful API response.
 *
 * <pre>
 * {
 *   "success": true,
 *   "message": "Invoice created successfully",
 *   "data": { ... },
 *   "timestamp": "2026-08-02T10:00:00Z",
 *   "correlationId": "..."
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        String correlationId) {

    public static <T> ApiResponse<T> of(String message, T data, String correlationId) {
        return new ApiResponse<>(true, message, data, Instant.now(), correlationId);
    }

    public static <T> ApiResponse<T> ok(T data, String correlationId) {
        return of("Success", data, correlationId);
    }

    public static ApiResponse<Void> message(String message, String correlationId) {
        return new ApiResponse<>(true, message, null, Instant.now(), correlationId);
    }
}
