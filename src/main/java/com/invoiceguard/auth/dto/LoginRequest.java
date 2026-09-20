package com.invoiceguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * {@code organizationId} is optional — required only if the user belongs to
 * more than one organisation and the login is otherwise ambiguous
 * (see {@code AuthService#login}).
 */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password, UUID organizationId) {}
