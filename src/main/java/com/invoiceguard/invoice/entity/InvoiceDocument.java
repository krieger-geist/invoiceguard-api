package com.invoiceguard.invoice.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadata for a file attached to an invoice. The file bytes themselves
 * never live in this table (or any relational table) — only
 * {@code storageLocation}, an opaque pointer resolved by whichever
 * {@code DocumentStorageService} implementation is active (local disk today;
 * S3/Azure/GCS are drop-in future implementations of the same interface).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_documents")
public class InvoiceDocument extends OrganizationScopedEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "generated_filename", nullable = false, length = 255)
    private String generatedFilename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "storage_location", nullable = false, length = 500)
    private String storageLocation;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
