package com.invoiceguard.invoice.repository;

import com.invoiceguard.invoice.entity.InvoiceStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceStatusHistoryRepository extends JpaRepository<InvoiceStatusHistory, UUID> {

    List<InvoiceStatusHistory> findByInvoiceIdOrderByCreatedAtAsc(UUID invoiceId);
}
