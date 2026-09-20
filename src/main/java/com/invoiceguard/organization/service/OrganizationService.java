package com.invoiceguard.organization.service;

import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.organization.entity.Organization;
import com.invoiceguard.organization.repository.OrganizationRepository;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public Organization getById(UUID organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", organizationId));
    }

    /**
     * Creates a new organisation with a URL-safe, globally unique slug derived
     * from its display name (e.g. "Acme Corp" -&gt; "acme-corp", or
     * "acme-corp-2" if that slug is already taken).
     */
    @Transactional
    public Organization create(String name, String planOrNull) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setSlug(generateUniqueSlug(name));
        organization.setPlan(planOrNull);
        return organizationRepository.save(organization);
    }

    private String generateUniqueSlug(String name) {
        String base = NON_SLUG_CHARS
                .matcher(name.toLowerCase(Locale.ROOT).trim())
                .replaceAll("-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "org";
        }
        String candidate = base;
        int suffix = 2;
        while (organizationRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
