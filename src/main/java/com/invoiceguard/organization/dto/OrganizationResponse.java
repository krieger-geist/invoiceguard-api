package com.invoiceguard.organization.dto;

import com.invoiceguard.organization.entity.OrganizationStatus;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id, String name, String slug, OrganizationStatus status, String plan, Instant createdAt) {}
