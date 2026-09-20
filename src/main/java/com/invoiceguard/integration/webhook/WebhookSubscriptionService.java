package com.invoiceguard.integration.webhook;

import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.security.OpaqueTokenService;
import com.invoiceguard.security.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository repository;
    private final OpaqueTokenService opaqueTokenService;
    private final TenantContext tenantContext;

    public WebhookSubscriptionService(
            WebhookSubscriptionRepository repository, OpaqueTokenService opaqueTokenService, TenantContext tenantContext) {
        this.repository = repository;
        this.opaqueTokenService = opaqueTokenService;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public WebhookSubscriptionCreatedResponse create(WebhookSubscriptionRequest request) {
        UUID organizationId = tenantContext.requireOrganizationId();

        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setOrganizationId(organizationId);
        subscription.setUrl(request.url());
        subscription.setEventTypes(String.join(",", request.eventTypes()));
        subscription.setSigningSecret(opaqueTokenService.generateToken());
        subscription.setActive(request.active());
        subscription = repository.save(subscription);

        return new WebhookSubscriptionCreatedResponse(
                subscription.getId(), subscription.getUrl(), request.eventTypes().stream().toList(),
                subscription.isActive(), subscription.getSigningSecret());
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscriptionResponse> listForOrganization() {
        UUID organizationId = tenantContext.requireOrganizationId();
        return repository.findByOrganizationId(organizationId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deactivate(UUID subscriptionId) {
        UUID organizationId = tenantContext.requireOrganizationId();
        WebhookSubscription subscription = repository
                .findByIdAndOrganizationId(subscriptionId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("WebhookSubscription", subscriptionId));
        subscription.setActive(false);
        repository.save(subscription);
    }

    private WebhookSubscriptionResponse toResponse(WebhookSubscription subscription) {
        return new WebhookSubscriptionResponse(
                subscription.getId(), subscription.getUrl(),
                List.of(subscription.getEventTypes().split(",")), subscription.isActive());
    }
}
