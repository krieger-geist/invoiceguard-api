package com.invoiceguard.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RolePermissionsTest {

    @Test
    void superAdminHasEveryPermission() {
        assertThat(RolePermissions.permissionsFor(Role.SUPER_ADMIN)).containsAll(java.util.List.of(Permission.values()));
    }

    @Test
    void onlyFinanceManagerAndOrgAdminCanApproveCriticalRiskInvoices() {
        for (Role role : Role.values()) {
            boolean canApproveCritical = RolePermissions.permissionsFor(role).contains(Permission.INVOICE_APPROVE_CRITICAL_RISK);
            boolean expected = role == Role.FINANCE_MANAGER || role == Role.ORGANIZATION_ADMIN || role == Role.SUPER_ADMIN;
            assertThat(canApproveCritical)
                    .as("Role %s critical-risk-approval permission", role)
                    .isEqualTo(expected);
        }
    }

    @Test
    void auditorCanReadAuditLogsButCannotWriteInvoices() {
        var permissions = RolePermissions.permissionsFor(Role.AUDITOR);
        assertThat(permissions).contains(Permission.AUDIT_READ);
        assertThat(permissions).doesNotContain(Permission.INVOICE_WRITE, Permission.INVOICE_APPROVE);
    }

    @Test
    void analystCannotApproveInvoices() {
        assertThat(RolePermissions.permissionsFor(Role.ANALYST)).doesNotContain(Permission.INVOICE_APPROVE);
    }

    @Test
    void reviewerCanApproveButNotAtElevatedRiskLevels() {
        var permissions = RolePermissions.permissionsFor(Role.REVIEWER);
        assertThat(permissions).contains(Permission.INVOICE_APPROVE);
        assertThat(permissions).doesNotContain(Permission.INVOICE_APPROVE_HIGH_RISK, Permission.INVOICE_APPROVE_CRITICAL_RISK);
    }

    @Test
    void apiClientHasOnlyTheThreeDocumentedScopes() {
        var permissions = RolePermissions.permissionsFor(Role.API_CLIENT);
        assertThat(permissions).containsExactlyInAnyOrder(Permission.INVOICE_READ, Permission.INVOICE_WRITE, Permission.VENDOR_READ);
    }
}
