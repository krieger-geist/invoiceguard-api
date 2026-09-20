package com.invoiceguard.organization.dto;

import com.invoiceguard.organization.entity.OrganizationMemberStatus;
import com.invoiceguard.security.Role;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        Role role,
        OrganizationMemberStatus status,
        Instant joinedAt) {}
