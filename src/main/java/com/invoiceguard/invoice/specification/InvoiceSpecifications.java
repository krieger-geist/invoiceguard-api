package com.invoiceguard.invoice.specification;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.invoice.entity.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable {@link Specification} predicates for invoice search. Every
 * caller MUST include {@link #belongsToOrganization} — see the equivalent
 * note on {@code VendorSpecifications} for why this is the repository-level
 * half of tenant isolation.
 */
public final class InvoiceSpecifications {

    private InvoiceSpecifications() {}

    public static Specification<Invoice> belongsToOrganization(UUID organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Invoice> hasVendor(UUID vendorId) {
        return (root, query, cb) -> vendorId == null ? null : cb.equal(root.get("vendorId"), vendorId);
    }

    public static Specification<Invoice> hasStatus(InvoiceStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Invoice> hasRiskLevel(InvoiceRiskLevel riskLevel) {
        return (root, query, cb) -> riskLevel == null ? null : cb.equal(root.get("riskLevel"), riskLevel);
    }

    public static Specification<Invoice> hasPurchaseOrderNumber(String purchaseOrderNumber) {
        return (root, query, cb) -> (purchaseOrderNumber == null || purchaseOrderNumber.isBlank())
                ? null
                : cb.equal(cb.upper(root.get("purchaseOrderNumber")), purchaseOrderNumber.toUpperCase());
    }

    public static Specification<Invoice> createdBy(String createdBy) {
        return (root, query, cb) ->
                (createdBy == null || createdBy.isBlank()) ? null : cb.equal(root.get("createdBy"), createdBy);
    }

    public static Specification<Invoice> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) {
                return null;
            }
            if (min != null && max != null) {
                return cb.between(root.get("totalAmount"), min, max);
            }
            return min != null
                    ? cb.greaterThanOrEqualTo(root.get("totalAmount"), min)
                    : cb.lessThanOrEqualTo(root.get("totalAmount"), max);
        };
    }

    public static Specification<Invoice> invoiceDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("invoiceDate"), from, to);
            }
            return from != null
                    ? cb.greaterThanOrEqualTo(root.get("invoiceDate"), from)
                    : cb.lessThanOrEqualTo(root.get("invoiceDate"), to);
        };
    }

    public static Specification<Invoice> dueDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("dueDate"), from, to);
            }
            return from != null
                    ? cb.greaterThanOrEqualTo(root.get("dueDate"), from)
                    : cb.lessThanOrEqualTo(root.get("dueDate"), to);
        };
    }

    public static Specification<Invoice> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            Predicate byInvoiceNumber = cb.like(cb.lower(root.get("invoiceNumber")), pattern);
            Predicate byDescription = cb.like(cb.lower(root.get("description")), pattern);
            Predicate byPaymentReference = cb.like(cb.lower(root.get("paymentReference")), pattern);
            return cb.or(byInvoiceNumber, byDescription, byPaymentReference);
        };
    }
}
