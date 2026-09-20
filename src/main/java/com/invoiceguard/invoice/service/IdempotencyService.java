package com.invoiceguard.invoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceguard.exception.IdempotencyConflictException;
import com.invoiceguard.invoice.entity.IdempotencyRecord;
import com.invoiceguard.invoice.repository.IdempotencyRecordRepository;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the {@code Idempotency-Key} contract from the spec: if the same
 * organisation sends the same key with the same request body, the original
 * result is returned rather than a new invoice being created; the same key
 * with a *different* body is rejected outright.
 *
 * <p>The request hash is computed by serializing the request DTO to canonical
 * JSON via Jackson and SHA-256-hashing that — same one-way hashing idea used
 * for tokens elsewhere in the app (see {@code OpaqueTokenService}), just
 * applied to a request payload instead of a secret, so this is deliberately
 * its own small utility rather than overloading the token service's name/intent.
 */
@Service
public class IdempotencyService {

    private static final int RECORD_TTL_HOURS = 24;

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String hashRequest(Object request) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash idempotent request body", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> find(UUID organizationId, String idempotencyKey) {
        return repository.findByOrganizationIdAndIdempotencyKey(organizationId, idempotencyKey);
    }

    /**
     * Validates a pre-existing record against the current request's hash.
     * Throws {@link IdempotencyConflictException} if the same key was reused
     * with a different request body — this is the "reject reuse with a
     * different body" rule from the spec.
     */
    public void assertMatches(IdempotencyRecord existing, String currentRequestHash) {
        if (!existing.getRequestHash().equals(currentRequestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key '" + existing.getIdempotencyKey() + "' was already used with a different request body");
        }
    }

    @Transactional
    public void save(UUID organizationId, String idempotencyKey, String requestHash, UUID responseReference, int httpStatus) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setOrganizationId(organizationId);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setResponseReference(responseReference);
        record.setHttpStatus(httpStatus);
        record.setExpiresAt(Instant.now().plus(RECORD_TTL_HOURS, ChronoUnit.HOURS));
        repository.save(record);
    }
}
