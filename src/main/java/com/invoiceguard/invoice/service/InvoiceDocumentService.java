package com.invoiceguard.invoice.service;

import com.invoiceguard.exception.BusinessRuleViolationException;
import com.invoiceguard.integration.storage.DocumentStorageService;
import com.invoiceguard.integration.storage.StoredDocument;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceDocument;
import com.invoiceguard.invoice.repository.InvoiceDocumentRepository;
import com.invoiceguard.security.TenantContext;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceDocumentService {

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("application/pdf", "image/png", "image/jpeg", "image/tiff");

    private final InvoiceDocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final InvoiceService invoiceService;
    private final TenantContext tenantContext;
    private final long maxFileSizeBytes;

    public InvoiceDocumentService(
            InvoiceDocumentRepository documentRepository,
            DocumentStorageService storageService,
            InvoiceService invoiceService,
            TenantContext tenantContext,
            @Value("${invoiceguard.documents.max-file-size-mb:20}") long maxFileSizeMb) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.invoiceService = invoiceService;
        this.tenantContext = tenantContext;
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
    }

    @Transactional
    public InvoiceDocument upload(UUID invoiceId, String originalFilename, String mimeType, long declaredSize, InputStream content) {
        Invoice invoice = invoiceService.getOwnedById(invoiceId); // tenant + existence check

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessRuleViolationException(
                    "Unsupported file type: " + mimeType + ". Allowed: " + ALLOWED_MIME_TYPES);
        }
        if (declaredSize > maxFileSizeBytes) {
            throw new BusinessRuleViolationException(
                    "File exceeds the maximum allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }

        StoredDocument stored = storageService.store(
                invoice.getOrganizationId().toString(), originalFilename, mimeType, content);

        if (stored.sizeBytes() > maxFileSizeBytes) {
            storageService.delete(stored.storageLocation());
            throw new BusinessRuleViolationException(
                    "File exceeds the maximum allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }

        InvoiceDocument document = new InvoiceDocument();
        document.setOrganizationId(invoice.getOrganizationId());
        document.setInvoiceId(invoiceId);
        document.setOriginalFilename(originalFilename);
        document.setGeneratedFilename(stored.generatedFilename());
        document.setMimeType(mimeType);
        document.setSizeBytes(stored.sizeBytes());
        document.setChecksum(stored.checksum());
        document.setStorageLocation(stored.storageLocation());
        document.setUploadedBy(tenantContext.requireUserId());

        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDocument> list(UUID invoiceId) {
        invoiceService.getOwnedById(invoiceId);
        return documentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
    }
}
