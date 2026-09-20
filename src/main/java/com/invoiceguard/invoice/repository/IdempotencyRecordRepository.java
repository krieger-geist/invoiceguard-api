package com.invoiceguard.invoice.repository;

import com.invoiceguard.invoice.entity.IdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);
}
