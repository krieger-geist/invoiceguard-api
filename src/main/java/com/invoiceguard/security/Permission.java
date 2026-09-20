package com.invoiceguard.security;

/**
 * Fine-grained permissions checked via {@code @PreAuthorize("hasAuthority('...')")}.
 * The authority string stored in the JWT and in Spring's {@code GrantedAuthority}
 * is {@link #getAuthority()}, e.g. {@code "invoice:write"}.
 *
 * <p>A user's effective permission set is the union of permissions granted by
 * their {@link Role} (see {@link RolePermissions}) — there is no per-user
 * permission override in v1; that would be a reasonable future enhancement
 * if a customer needs finer-grained control than role-based access provides.
 */
public enum Permission {
    ORGANIZATION_MANAGE("organization:manage"),
    USER_MANAGE("user:manage"),

    VENDOR_READ("vendor:read"),
    VENDOR_WRITE("vendor:write"),
    VENDOR_VERIFY("vendor:verify"),
    VENDOR_BANK_CHANGE_APPROVE("vendor:bank-change-approve"),

    INVOICE_READ("invoice:read"),
    INVOICE_WRITE("invoice:write"),
    INVOICE_APPROVE("invoice:approve"),
    INVOICE_APPROVE_HIGH_RISK("invoice:approve-high-risk"),
    INVOICE_APPROVE_CRITICAL_RISK("invoice:approve-critical-risk"),

    RISK_RULE_CONFIGURE("risk:configure"),

    ALERT_READ("alert:read"),
    ALERT_MANAGE("alert:manage"),

    ANALYTICS_READ("analytics:read"),

    AUDIT_READ("audit:read"),

    API_KEY_MANAGE("api-key:manage");

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}
