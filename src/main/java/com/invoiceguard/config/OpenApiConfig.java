package com.invoiceguard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata and security schemes. Full endpoint-level documentation
 * (request/response examples, error codes per endpoint) is layered on module
 * by module via {@code @Operation}/{@code @ApiResponse} annotations — see the
 * Phase 8 Swagger polish pass.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String API_KEY_SCHEME = "apiKeyAuth";

    @Bean
    public OpenAPI invoiceGuardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("InvoiceGuard API")
                        .description("Secure, multi-tenant invoice-risk analysis platform. "
                                + "Detects duplicate invoices, suspicious vendor activity, altered bank "
                                + "details, unusual amounts, tax mismatches, and possible fraud before "
                                + "payment approval.\n\n"
                                + "**Authentication**: Bearer JWT (`/auth/login`) for interactive users, or an API key "
                                + "via the `X-API-Key` header for machine-to-machine access.\n\n"
                                + "**Roles**: SUPER_ADMIN, ORGANIZATION_ADMIN, FINANCE_MANAGER, ANALYST, REVIEWER, "
                                + "AUDITOR, API_CLIENT — see RolePermissions in source for the exact permission each holds.\n\n"
                                + "**Idempotency**: invoice creation honors an `Idempotency-Key` header — the same key "
                                + "with the same body replays the original result; the same key with a different body "
                                + "returns 409 IDEMPOTENCY_KEY_REUSED.\n\n"
                                + "**Risk scores**: 0-29 LOW, 30-59 MEDIUM, 60-79 HIGH, 80-100 CRITICAL (configurable). "
                                + "Every score is backed by explainable findings — see GET /invoices/{id}/risk-assessment.\n\n"
                                + "**Pagination**: list endpoints accept `page`/`size`/`sort` and return "
                                + "{content, page, size, totalElements, totalPages, first, last}.\n\n"
                                + "**Errors**: every error response is {success:false, code, message, fieldErrors, "
                                + "timestamp, path, correlationId} — `code` is a stable machine-readable value from "
                                + "the ErrorCode enum (e.g. INVOICE_NOT_FOUND, ACCESS_DENIED, OPTIMISTIC_LOCK_CONFLICT).")
                        .version("v1")
                        .contact(new Contact().name("InvoiceGuard Engineering"))
                        .license(new License().name("Apache 2.0")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token obtained from /api/v1/auth/login"))
                        .addSecuritySchemes(
                                API_KEY_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("API key for machine-to-machine access")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
    }
}
