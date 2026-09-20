package com.invoiceguard.exception;

/**
 * Thrown when a caller attempts to access or mutate data belonging to an
 * organisation other than their own. This is the enforcement point for
 * multi-tenant isolation at the service layer.
 */
public class TenantAccessDeniedException extends ApplicationException {

    public TenantAccessDeniedException(String message) {
        super(ErrorCode.TENANT_MISMATCH, message);
    }

    public static TenantAccessDeniedException forEntity(String entityName, Object id) {
        return new TenantAccessDeniedException(
                "%s [%s] does not belong to the caller's organisation".formatted(entityName, id));
    }
}
