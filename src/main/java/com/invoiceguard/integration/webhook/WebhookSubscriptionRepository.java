package com.invoiceguard.integration.webhook;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {

    List<WebhookSubscription> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<WebhookSubscription> findByOrganizationId(UUID organizationId);

    Optional<WebhookSubscription> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
