package com.invoiceguard.auth.dto;

import java.util.UUID;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresInSeconds,
        UUID userId,
        UUID organizationId,
        String role) {

    public static AuthTokenResponse bearer(
            String accessToken, String refreshToken, int expiresInSeconds, UUID userId, UUID organizationId, String role) {
        return new AuthTokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, userId, organizationId, role);
    }
}
