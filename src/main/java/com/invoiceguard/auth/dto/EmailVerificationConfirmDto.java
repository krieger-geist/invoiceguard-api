package com.invoiceguard.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmDto(@NotBlank String token) {}
