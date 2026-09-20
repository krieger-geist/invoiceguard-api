package com.invoiceguard.integration.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Default {@link DocumentStorageService}: writes files to a local directory,
 * namespaced per organisation. Suitable for local development and single-node
 * deployments; a cloud deployment would swap this for an S3/Blob/GCS-backed
 * implementation of the same interface — nothing else in the app would change.
 */
@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    private final Path baseDirectory;

    public LocalDocumentStorageService(@Value("${invoiceguard.documents.local-storage-path:./data/documents}") String basePath) {
        this.baseDirectory = Path.of(basePath);
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create document storage directory: " + basePath, e);
        }
    }

    @Override
    public StoredDocument store(String organizationId, String originalFilename, String mimeType, InputStream content) {
        try {
            Path orgDirectory = baseDirectory.resolve(organizationId);
            Files.createDirectories(orgDirectory);

            String extension = extractExtension(originalFilename);
            String generatedFilename = UUID.randomUUID() + extension;
            Path targetPath = orgDirectory.resolve(generatedFilename);

            byte[] bytes = content.readAllBytes();
            Files.write(targetPath, bytes);

            return new StoredDocument(generatedFilename, targetPath.toString(), sha256Hex(bytes), bytes.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store document: " + originalFilename, e);
        }
    }

    @Override
    public InputStream retrieve(String storageLocation) {
        try {
            return Files.newInputStream(Path.of(storageLocation));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read document at: " + storageLocation, e);
        }
    }

    @Override
    public void delete(String storageLocation) {
        try {
            Files.deleteIfExists(Path.of(storageLocation));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete document at: " + storageLocation, e);
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }
}
