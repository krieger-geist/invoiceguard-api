package com.invoiceguard.audit.service;

import com.invoiceguard.audit.entity.AuditEvent;
import com.invoiceguard.audit.entity.AuditResult;
import com.invoiceguard.audit.repository.AuditEventRepository;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.security.TenantContext;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two responsibilities: writing immutable {@link AuditEvent} rows (called
 * only from {@code AuditEventListener}, never directly from business
 * services — this keeps "what gets audited" defined in exactly one place)
 * and reading them back for {@code GET /audit-logs}.
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final TenantContext tenantContext;

    public AuditService(AuditEventRepository repository, TenantContext tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public void record(
            UUID organizationId, String action, String entityType, UUID entityId, String newValues,
            String ipAddress, AuditResult result, String failureReason) {
        AuditEvent event = new AuditEvent();
        event.setOrganizationId(organizationId);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setNewValues(newValues);
        event.setIpAddress(ipAddress);
        event.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        event.setResult(result);
        event.setFailureReason(failureReason);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> search(String action, String entityType, LocalDate from, LocalDate to, Pageable pageable) {
        UUID organizationId = tenantContext.requireOrganizationId();
        Specification<AuditEvent> spec = Specification.<AuditEvent>where(
                        (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId))
                .and((root, query, cb) -> (action == null || action.isBlank()) ? null : cb.equal(root.get("action"), action))
                .and((root, query, cb) -> (entityType == null || entityType.isBlank()) ? null : cb.equal(root.get("entityType"), entityType))
                .and((root, query, cb) -> from == null
                        ? null
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()))
                .and((root, query, cb) -> to == null
                        ? null
                        : cb.lessThanOrEqualTo(root.get("createdAt"), to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
        return repository.findAll(spec, pageable);
    }
}
