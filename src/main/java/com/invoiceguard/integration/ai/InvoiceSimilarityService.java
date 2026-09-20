package com.invoiceguard.integration.ai;

/**
 * Abstraction over "how similar are these two pieces of invoice text"
 * (descriptions, line-item text). {@code DefaultInvoiceSimilarityService}
 * backs this with the same deterministic word-overlap logic already used by
 * {@code DuplicateDetectionService} and {@code DescriptionMismatchRule} — an
 * embedding-based AI implementation could replace it later for subtler
 * semantic matches without changing any caller.
 */
public interface InvoiceSimilarityService {

    double similarity(String textA, String textB);
}
