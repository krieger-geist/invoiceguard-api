package com.invoiceguard.invoice.repository;

import com.invoiceguard.invoice.entity.InvoiceDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, UUID> {

    List<InvoiceDocument> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
