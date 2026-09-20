package com.invoiceguard.security;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Static Role -&gt; Permission mapping. Kept as code rather than DB rows
 * because the role set is fixed and this mapping needs to be resolvable
 * synchronously at JWT-issuance time without a query; if per-organisation
 * customizable roles become a requirement, this is the seam where that
 * would be replaced by a database-backed lookup without touching callers
 * (they only ever call {@link #permissionsFor(Role)}).
 */
public final class RolePermissions {

    private static final Map<Role, Set<Permission>> MAP = new EnumMap<>(Role.class);

    static {
        MAP.put(Role.SUPER_ADMIN, EnumSet.allOf(Permission.class));

        MAP.put(
                Role.ORGANIZATION_ADMIN,
                EnumSet.of(
                        Permission.ORGANIZATION_MANAGE,
                        Permission.USER_MANAGE,
                        Permission.VENDOR_READ,
                        Permission.VENDOR_WRITE,
                        Permission.VENDOR_VERIFY,
                        Permission.VENDOR_BANK_CHANGE_APPROVE,
                        Permission.INVOICE_READ,
                        Permission.INVOICE_WRITE,
                        Permission.INVOICE_APPROVE,
                        Permission.INVOICE_APPROVE_HIGH_RISK,
                        Permission.INVOICE_APPROVE_CRITICAL_RISK,
                        Permission.RISK_RULE_CONFIGURE,
                        Permission.ALERT_READ,
                        Permission.ALERT_MANAGE,
                        Permission.ANALYTICS_READ,
                        Permission.AUDIT_READ,
                        Permission.API_KEY_MANAGE));

        MAP.put(
                Role.FINANCE_MANAGER,
                EnumSet.of(
                        Permission.VENDOR_READ,
                        Permission.VENDOR_BANK_CHANGE_APPROVE,
                        Permission.INVOICE_READ,
                        Permission.INVOICE_WRITE,
                        Permission.INVOICE_APPROVE,
                        Permission.INVOICE_APPROVE_HIGH_RISK,
                        Permission.INVOICE_APPROVE_CRITICAL_RISK,
                        Permission.ALERT_READ,
                        Permission.ALERT_MANAGE,
                        Permission.ANALYTICS_READ));

        MAP.put(
                Role.ANALYST,
                EnumSet.of(
                        Permission.VENDOR_READ,
                        Permission.INVOICE_READ,
                        Permission.INVOICE_WRITE,
                        Permission.ALERT_READ,
                        Permission.ANALYTICS_READ));

        MAP.put(
                Role.REVIEWER,
                EnumSet.of(
                        Permission.VENDOR_READ,
                        Permission.INVOICE_READ,
                        Permission.INVOICE_APPROVE,
                        Permission.ALERT_READ));

        MAP.put(
                Role.AUDITOR,
                EnumSet.of(
                        Permission.VENDOR_READ,
                        Permission.INVOICE_READ,
                        Permission.ALERT_READ,
                        Permission.ANALYTICS_READ,
                        Permission.AUDIT_READ));

        MAP.put(
                Role.API_CLIENT,
                EnumSet.of(Permission.INVOICE_READ, Permission.INVOICE_WRITE, Permission.VENDOR_READ));
    }

    private RolePermissions() {}

    public static Set<Permission> permissionsFor(Role role) {
        return MAP.getOrDefault(role, Set.of());
    }
}
