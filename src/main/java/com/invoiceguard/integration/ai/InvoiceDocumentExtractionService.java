package com.invoiceguard.integration.ai;

import java.io.InputStream;
import java.util.Optional;

/**
 * Abstraction for extracting structured invoice data from an uploaded
 * document (PDF/image). No implementation is wired into the invoice-creation
 * flow yet — invoices are still entered structurally via
 * {@code InvoiceCreateRequest} — this interface exists so a future OCR/AI
 * provider (Gemini's vision capabilities, or a dedicated OCR service) can be
 * plugged in later without any change to the invoice module itself, per the
 * spec's "AI-ready but functional without AI" requirement.
 */
public interface InvoiceDocumentExtractionService {

    Optional<ExtractedInvoiceData> extract(InputStream documentContent, String mimeType);
}
