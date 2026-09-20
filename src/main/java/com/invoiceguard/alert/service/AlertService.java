package com.invoiceguard.alert.service;

import com.invoiceguard.alert.entity.Alert;
import com.invoiceguard.alert.entity.AlertStatus;
import com.invoiceguard.alert.entity.AlertType;
import com.invoiceguard.alert.event.AlertCreatedEvent;
import com.invoiceguard.alert.repository.AlertRepository;
import com.invoiceguard.exception.BusinessRuleViolationException;
import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.security.TenantContext;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final TenantContext tenantContext;
    private final ApplicationEventPublisher eventPublisher;

    public AlertService(AlertRepository alertRepository, TenantContext tenantContext, ApplicationEventPublisher eventPublisher) {
        this.alertRepository = alertRepository;
        this.tenantContext = tenantContext;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Used only by {@code AlertEventListener}. Deduplicates: if an OPEN or
     * ACKNOWLEDGED alert of the same type already exists for the same
     * related entity, no new row is created — this stops, for example, a
     * vendor with three bank-change attempts in a row from generating three
     * separate VENDOR_BANK_CHANGED alerts instead of one that stays open
     * until someone actually looks at it.
     */
    @Transactional
    public void raise(
            UUID organizationId, AlertType type, RiskFindingSeverity severity, String title, String message,
            String relatedEntityType, UUID relatedEntityId) {
        if (relatedEntityId != null
                && alertRepository.existsByOrganizationIdAndTypeAndRelatedEntityIdAndStatusNot(
                        organizationId, type, relatedEntityId, AlertStatus.RESOLVED)) {
            return;
        }

        Alert alert = new Alert();
        alert.setOrganizationId(organizationId);
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setStatus(AlertStatus.OPEN);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setRelatedEntityType(relatedEntityType);
        alert.setRelatedEntityId(relatedEntityId);
        alert = alertRepository.save(alert);
        eventPublisher.publishEvent(new AlertCreatedEvent(alert.getId(), organizationId, type.name(), severity.name(), title));
    }

    @Transactional(readOnly = true)
    public Page<Alert> search(AlertType type, AlertStatus status, Pageable pageable) {
        UUID organizationId = tenantContext.requireOrganizationId();
        Specification<Alert> spec = Specification.<Alert>where(
                        (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId))
                .and((root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type))
                .and((root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status));
        return alertRepository.findAll(spec, pageable);
    }

    @Transactional
    public Alert acknowledge(UUID alertId) {
        Alert alert = getOwned(alertId);
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new BusinessRuleViolationException("Only OPEN alerts can be acknowledged");
        }
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(tenantContext.requireUserId());
        alert.setAcknowledgedAt(Instant.now());
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert resolve(UUID alertId) {
        Alert alert = getOwned(alertId);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new BusinessRuleViolationException("This alert is already resolved");
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(tenantContext.requireUserId());
        alert.setResolvedAt(Instant.now());
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert dismiss(UUID alertId) {
        Alert alert = getOwned(alertId);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new BusinessRuleViolationException("This alert is already resolved and cannot be dismissed");
        }
        alert.setStatus(AlertStatus.DISMISSED);
        return alertRepository.save(alert);
    }

    private Alert getOwned(UUID alertId) {
        UUID organizationId = tenantContext.requireOrganizationId();
        return alertRepository
                .findByIdAndOrganizationId(alertId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Alert", alertId));
    }
}
