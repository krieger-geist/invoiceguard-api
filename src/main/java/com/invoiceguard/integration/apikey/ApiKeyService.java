package com.invoiceguard.integration.apikey;

import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.security.OpaqueTokenService;
import com.invoiceguard.security.Permission;
import com.invoiceguard.security.TenantContext;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "igk_";

    private final ApiKeyRepository repository;
    private final OpaqueTokenService opaqueTokenService;
    private final TenantContext tenantContext;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ApiKeyService(
            ApiKeyRepository repository,
            OpaqueTokenService opaqueTokenService,
            TenantContext tenantContext,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.opaqueTokenService = opaqueTokenService;
        this.tenantContext = tenantContext;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ApiKeyGeneratedResponse generate(ApiKeyCreateRequest request) {
        UUID organizationId = tenantContext.requireOrganizationId();

        String rawKey = KEY_PREFIX + opaqueTokenService.generateToken();
        String displayPrefix = rawKey.substring(0, Math.min(rawKey.length(), 12));

        ApiKey apiKey = new ApiKey();
        apiKey.setOrganizationId(organizationId);
        apiKey.setName(request.name());
        apiKey.setKeyPrefix(displayPrefix);
        apiKey.setHashedKey(opaqueTokenService.hash(rawKey));
        apiKey.setScopes(request.scopes().stream().map(Permission::getAuthority).collect(Collectors.joining(",")));
        apiKey.setExpiresAt(request.expiresAt());
        apiKey.setRateLimitPerMinute(request.rateLimitPerMinute());
        apiKey = repository.save(apiKey);
        eventPublisher.publishEvent(new ApiKeyGeneratedEvent(apiKey.getId(), organizationId, apiKey.getName()));

        return new ApiKeyGeneratedResponse(
                apiKey.getId(), apiKey.getName(), rawKey, apiKey.getKeyPrefix(), scopesList(apiKey), apiKey.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listForOrganization() {
        UUID organizationId = tenantContext.requireOrganizationId();
        return repository.findByOrganizationId(organizationId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void revoke(UUID apiKeyId) {
        UUID organizationId = tenantContext.requireOrganizationId();
        ApiKey apiKey = repository
                .findByIdAndOrganizationId(apiKeyId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("ApiKey", apiKeyId));
        apiKey.setRevoked(true);
        apiKey.setRevokedAt(Instant.now());
        repository.save(apiKey);
    }

    /** Used by {@code ApiKeyAuthenticationFilter} — not tenant-scoped since the caller doesn't have a tenant context yet. */
    @Transactional
    public java.util.Optional<ApiKey> authenticate(String rawKey) {
        String hash = opaqueTokenService.hash(rawKey);
        return repository.findByHashedKey(hash).filter(ApiKey::isActive).map(apiKey -> {
            apiKey.setLastUsedAt(Instant.now());
            return repository.save(apiKey);
        });
    }

    public static Set<Permission> parseScopes(String scopesCsv) {
        return Arrays.stream(scopesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(authority -> Arrays.stream(Permission.values())
                        .filter(p -> p.getAuthority().equals(authority))
                        .findFirst()
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<String> scopesList(ApiKey apiKey) {
        return Arrays.stream(apiKey.getScopes().split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix(), scopesList(apiKey), apiKey.isRevoked(),
                apiKey.getExpiresAt(), apiKey.getLastUsedAt(), apiKey.getCreatedAt());
    }
}
