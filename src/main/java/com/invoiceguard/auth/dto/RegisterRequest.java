package com.invoiceguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-service signup: creates a new organisation AND its first user
 * (as ORGANIZATION_ADMIN) in one request. Inviting additional members into
 * an existing organisation is a separate flow (OrganizationMemberController).
 */
public record RegisterRequest(
        @NotBlank @Size(max = 255) String organizationName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
                @Size(min = 12, max = 128)
                @Pattern(
                        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                        message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
                String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {}
