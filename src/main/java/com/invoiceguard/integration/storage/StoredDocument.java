package com.invoiceguard.integration.storage;

/**
 * Result of a successful store operation: everything the caller needs to
 * persist on an {@code InvoiceDocument} row. {@code storageLocation} is
 * intentionally opaque to callers — only the storage implementation itself
 * knows how to turn it back into retrievable bytes.
 */
public record StoredDocument(String generatedFilename, String storageLocation, String checksum, long sizeBytes) {}
