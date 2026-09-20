package com.invoiceguard.risk.service;

import com.invoiceguard.risk.dto.RiskRuleConfigResponse;
import com.invoiceguard.risk.entity.RiskRuleCode;
import com.invoiceguard.risk.entity.RiskRuleConfiguration;
import com.invoiceguard.risk.repository.RiskRuleConfigurationRepository;
import com.invoiceguard.security.TenantContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets an organisation admin see and override the effective config for every
 * rule the engine knows about — including rules that have never been
 * customized, which are reported with their implicit defaults
 * (enabled=true, {@code usingDefaultWeight}=true) rather than being omitted,
 * so the API always returns a complete picture of all
 * {@link RiskRuleCode#values()}.
 */
@Service
public class RiskRuleConfigService {

    private static final String CACHE_NAME = "riskRuleConfig";

    private final RiskRuleConfigurationRepository repository;
    private final TenantContext tenantContext;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    public RiskRuleConfigService(
            RiskRuleConfigurationRepository repository,
            TenantContext tenantContext,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            CacheManager cacheManager) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
    }

    /**
     * The hot-path lookup {@code RiskEngine} calls on every single invoice
     * analysis — cached in Redis (short TTL, see {@code RedisConfig}) since
     * rule configuration changes rarely but is read constantly. Returns a
     * plain serializable snapshot record rather than the JPA entity itself,
     * so caching never has to worry about Hibernate proxy/lazy-loading
     * serialization pitfalls.
     */
    @Cacheable(CACHE_NAME)
    public Map<RiskRuleCode, RuleConfigSnapshot> getEffectiveConfigSnapshot(UUID organizationId) {
        Map<RiskRuleCode, RuleConfigSnapshot> snapshot = new HashMap<>();
        for (RiskRuleConfiguration config : repository.findByOrganizationId(organizationId)) {
            snapshot.put(config.getRuleCode(), new RuleConfigSnapshot(config.isEnabled(), config.getWeightPoints()));
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    public java.util.List<RiskRuleConfigResponse> listAll() {
        UUID organizationId = tenantContext.requireOrganizationId();
        Map<RiskRuleCode, RiskRuleConfiguration> existing = repository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(RiskRuleConfiguration::getRuleCode, c -> c));

        return Arrays.stream(RiskRuleCode.values())
                .map(code -> {
                    RiskRuleConfiguration config = existing.get(code);
                    if (config == null) {
                        return new RiskRuleConfigResponse(code, true, null, true);
                    }
                    return new RiskRuleConfigResponse(code, config.isEnabled(), config.getWeightPoints(), config.getWeightPoints() == null);
                })
                .toList();
    }

    @Transactional
    public RiskRuleConfigResponse update(RiskRuleCode ruleCode, boolean enabled, Integer weightPoints) {
        UUID organizationId = tenantContext.requireOrganizationId();
        RiskRuleConfiguration config = repository
                .findByOrganizationIdAndRuleCode(organizationId, ruleCode)
                .orElseGet(() -> {
                    RiskRuleConfiguration created = new RiskRuleConfiguration();
                    created.setOrganizationId(organizationId);
                    created.setRuleCode(ruleCode);
                    return created;
                });
        config.setEnabled(enabled);
        config.setWeightPoints(weightPoints);
        config = repository.save(config);
        java.util.Optional.ofNullable(cacheManager.getCache(CACHE_NAME)).ifPresent(cache -> cache.evict(organizationId));
        eventPublisher.publishEvent(new com.invoiceguard.risk.event.RiskRuleConfigChangedEvent(
                organizationId, ruleCode, enabled, weightPoints, tenantContext.requireUserId()));
        return new RiskRuleConfigResponse(ruleCode, config.isEnabled(), config.getWeightPoints(), config.getWeightPoints() == null);
    }
}
