package com.invoiceguard.invoice.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An immutable record of one status change. Rows are never updated or
 * deleted once written — {@code createdAt} (inherited) doubles as the
 * "changed at" timestamp, and {@code createdBy} (inherited, populated by
 * {@code AuditingConfig}) doubles as "changed by", so no redundant columns
 * are needed for either.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_status_history")
public class InvoiceStatusHistory extends OrganizationScopedEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private InvoiceStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private InvoiceStatus toStatus;

    @Column(name = "reason", length = 1000)
    private String reason;
}
