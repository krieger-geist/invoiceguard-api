package com.invoiceguard.invoice.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import com.invoiceguard.common.enums.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * A vendor invoice submitted for risk analysis and approval.
 *
 * <p>{@code normalizedInvoiceNumber} (see {@code InvoiceNumberNormalizer}) is
 * computed at creation time specifically so the duplicate-detection engine
 * (Phase 5) can compare invoice numbers without repeating normalization
 * logic per query — it's a denormalized, indexed column precisely because
 * it will be queried heavily.
 *
 * <p>{@code @SQLRestriction("deleted = false")} makes every query Hibernate
 * generates for this entity (including {@code findById}) automatically
 * exclude soft-deleted rows — callers never need to remember to add that
 * filter themselves. This is introduced here rather than on
 * {@code BaseEntity} globally because Invoice is the first entity with an
 * actual soft-delete endpoint; earlier modules can adopt the same
 * annotation when they gain one.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
@SQLRestriction("deleted = false")
public class Invoice extends OrganizationScopedEntity {

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "normalized_invoice_number", nullable = false, length = 100)
    private String normalizedInvoiceNumber;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "purchase_order_number", length = 100)
    private String purchaseOrderNumber;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 30)
    private InvoiceRiskLevel riskLevel;

    @Column(name = "latest_risk_score")
    private Integer latestRiskScore;

    @Column(name = "document_hash", length = 128)
    private String documentHash;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;
}
