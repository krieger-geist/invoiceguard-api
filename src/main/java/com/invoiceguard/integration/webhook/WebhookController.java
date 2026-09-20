package com.invoiceguard.integration.webhook;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Restricted to {@code organization:manage} — webhook endpoints are an org-wide integration setting, like API keys. */
@RestController
@RequestMapping("/api/v1/webhooks")
@PreAuthorize("hasAuthority('organization:manage')")


@Tag(name = "Webhooks", description = "Outbound event notifications (HMAC-signed) for invoice/vendor/alert lifecycle events.")
public class WebhookController {

    private final WebhookSubscriptionService service;

    public WebhookController(WebhookSubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WebhookSubscriptionResponse>> list(HttpServletRequest httpRequest) {
        return ApiResponse.ok(service.listForOrganization(), correlationId(httpRequest));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WebhookSubscriptionCreatedResponse>> create(
            @Valid @RequestBody WebhookSubscriptionRequest request, HttpServletRequest httpRequest) {
        WebhookSubscriptionCreatedResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        "Webhook created — store the signing secret now, it cannot be retrieved again",
                        response,
                        correlationId(httpRequest)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable UUID id, HttpServletRequest httpRequest) {
        service.deactivate(id);
        return ApiResponse.message("Webhook deactivated", correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
