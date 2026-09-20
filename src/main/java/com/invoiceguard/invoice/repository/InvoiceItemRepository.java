package com.invoiceguard.invoice.repository;

import com.invoiceguard.invoice.entity.InvoiceItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    List<InvoiceItem> findByInvoiceIdOrderByCreatedAtAsc(UUID invoiceId);

    void deleteByInvoiceId(UUID invoiceId);
}
