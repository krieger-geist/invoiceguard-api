package com.invoiceguard.user.dto;

import com.invoiceguard.user.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt) {}
