package com.invoiceguard.integration.ai;

import java.io.InputStream;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Default implementation: extraction is not yet available. Documents remain metadata-only (see InvoiceDocumentService). */
@Service
public class NoOpInvoiceDocumentExtractionService implements InvoiceDocumentExtractionService {

    @Override
    public Optional<ExtractedInvoiceData> extract(InputStream documentContent, String mimeType) {
        return Optional.empty();
    }
}
