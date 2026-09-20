package com.invoiceguard.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.invoiceguard.approval.entity.ApprovalPolicy;
import com.invoiceguard.approval.repository.ApprovalPolicyRepository;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.security.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalPolicyServiceTest {

    @Mock private ApprovalPolicyRepository repository;
    @Mock private TenantContext tenantContext;

    private ApprovalPolicyService service;

    private final UUID orgId = UUID.randomUUID();

    private ApprovalPolicy policy(String min, String max, int required) {
        ApprovalPolicy policy = new ApprovalPolicy();
        policy.setOrganizationId(orgId);
        policy.setMinAmount(new BigDecimal(min));
        policy.setMaxAmount(max == null ? null : new BigDecimal(max));
        policy.setRequiredApprovals(required);
        policy.setActive(true);
        return policy;
    }

    private Invoice invoiceWithAmount(String amount) {
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(orgId);
        invoice.setTotalAmount(new BigDecimal(amount));
        return invoice;
    }

    @Test
    void defaultsToOneApprovalWhenNoPoliciesConfigured() {
        service = new ApprovalPolicyService(repository, tenantContext);
        when(repository.findByOrganizationIdAndActiveTrueOrderByMinAmountAsc(orgId)).thenReturn(List.of());

        assertThat(service.resolveRequiredApprovals(invoiceWithAmount("5000"))).isEqualTo(1);
    }

    @Test
    void selectsTheMatchingAmountTier() {
        service = new ApprovalPolicyService(repository, tenantContext);
        List<ApprovalPolicy> policies = List.of(
                policy("0", "25000", 1),
                policy("25000", "100000", 2),
                policy("100000", null, 3));
        when(repository.findByOrganizationIdAndActiveTrueOrderByMinAmountAsc(orgId)).thenReturn(policies);

        assertThat(service.resolveRequiredApprovals(invoiceWithAmount("10000"))).isEqualTo(1);
        assertThat(service.resolveRequiredApprovals(invoiceWithAmount("50000"))).isEqualTo(2);
        assertThat(service.resolveRequiredApprovals(invoiceWithAmount("150000"))).isEqualTo(3);
    }

    @Test
    void highRiskInvoicesRequireOneAdditionalApprover() {
        service = new ApprovalPolicyService(repository, tenantContext);
        when(repository.findByOrganizationIdAndActiveTrueOrderByMinAmountAsc(orgId))
                .thenReturn(List.of(policy("0", null, 1)));

        Invoice invoice = invoiceWithAmount("5000");
        invoice.setRiskLevel(InvoiceRiskLevel.HIGH);

        assertThat(service.resolveRequiredApprovals(invoice)).isEqualTo(2);
    }

    @Test
    void lowRiskInvoicesDoNotGetTheExtraApprover() {
        service = new ApprovalPolicyService(repository, tenantContext);
        when(repository.findByOrganizationIdAndActiveTrueOrderByMinAmountAsc(orgId))
                .thenReturn(List.of(policy("0", null, 1)));

        Invoice invoice = invoiceWithAmount("5000");
        invoice.setRiskLevel(InvoiceRiskLevel.LOW);

        assertThat(service.resolveRequiredApprovals(invoice)).isEqualTo(1);
    }
}
