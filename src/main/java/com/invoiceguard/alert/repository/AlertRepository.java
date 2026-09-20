package com.invoiceguard.alert.repository;

import com.invoiceguard.alert.entity.Alert;
import com.invoiceguard.alert.entity.AlertType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {

    Optional<Alert> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndTypeAndRelatedEntityIdAndStatusNot(
            UUID organizationId, AlertType type, UUID relatedEntityId, com.invoiceguard.alert.entity.AlertStatus excludedStatus);
}
