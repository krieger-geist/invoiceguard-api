package com.invoiceguard.exception;

import com.invoiceguard.common.response.ErrorResponse;
import com.invoiceguard.common.response.ErrorResponse.FieldErrorItem;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central exception -&gt; HTTP response translator.
 *
 * <p>No controller in the codebase should be catching business exceptions
 * itself; they propagate here so that every endpoint returns the same
 * {@link ErrorResponse} shape. Stack traces are logged server-side only and
 * never included in the response body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- Application (business) exceptions --------------------------------

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException ex, HttpServletRequest request) {
        logAtAppropriateLevel(ex, ex.getErrorCode().getHttpStatus());
        return build(ex.getErrorCode(), ex.getMessage(), request);
    }

    // ---- Bean Validation ----------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorItem)
                .toList();
        ErrorResponse body = ErrorResponse.ofValidation(
                ErrorCode.VALIDATION_FAILED.name(),
                "One or more fields are invalid",
                fieldErrors,
                request.getRequestURI(),
                correlationId(request));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(body);
    }

    private FieldErrorItem toFieldErrorItem(FieldError fieldError) {
        return new FieldErrorItem(
                fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, "Malformed request body", request);
    }

    // ---- Security -------------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
        return build(ErrorCode.AUTHENTICATION_FAILED, "Invalid credentials", request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCOUNT_LOCKED, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.AUTHENTICATION_FAILED, "Authentication is required", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return build(ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action", request);
    }

    // ---- Persistence ------------------------------------------------------------

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(HttpServletRequest request) {
        return build(
                ErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "The resource was modified by another request. Please reload and try again.",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return build(ErrorCode.DATA_INTEGRITY_VIOLATION, "The request violates a data constraint", request);
    }

    // ---- Routing / method ------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(
                        "METHOD_NOT_ALLOWED", ex.getMessage(), request.getRequestURI(), correlationId(request)));
    }

    // ---- Fallback -----------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    // ---- helpers --------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                code.name(), message, request.getRequestURI(), correlationId(request));
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute("correlationId");
        return attr != null ? attr.toString() : request.getHeader("X-Correlation-Id");
    }

    private void logAtAppropriateLevel(ApplicationException ex, HttpStatus status) {
        if (status.is5xxServerError()) {
            log.error("Application exception: {}", ex.getMessage(), ex);
        } else {
            log.debug("Application exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        }
    }
}
