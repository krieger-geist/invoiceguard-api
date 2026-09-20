package com.invoiceguard.integration.storage;

import java.io.InputStream;

/**
 * Storage abstraction for invoice document files. The rest of the codebase
 * depends only on this interface, never on a concrete storage technology —
 * {@link LocalDocumentStorageService} is the only implementation today, but
 * AWS S3 / Azure Blob / GCS implementations are meant to be added later as
 * additional {@code @Service}s selected by configuration, with zero changes
 * to {@code InvoiceDocumentService} or any controller.
 */
public interface DocumentStorageService {

    /**
     * Stores the given content under a generated, collision-resistant name
     * and returns everything needed to retrieve it again later.
     */
    StoredDocument store(String organizationId, String originalFilename, String mimeType, InputStream content);

    /** Opens the stored content for reading. Caller is responsible for closing the stream. */
    InputStream retrieve(String storageLocation);

    void delete(String storageLocation);
}
