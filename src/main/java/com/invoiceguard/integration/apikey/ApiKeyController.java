package com.invoiceguard.integration.apikey;

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

@RestController
@RequestMapping("/api/v1/api-keys")
@PreAuthorize("hasAuthority('api-key:manage')")


@Tag(name = "API Keys", description = "Machine-to-machine credentials for API_CLIENT-scoped access.")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ApiResponse<List<ApiKeyResponse>> list(HttpServletRequest httpRequest) {
        return ApiResponse.ok(apiKeyService.listForOrganization(), correlationId(httpRequest));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyGeneratedResponse>> create(
            @Valid @RequestBody ApiKeyCreateRequest request, HttpServletRequest httpRequest) {
        ApiKeyGeneratedResponse response = apiKeyService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        "API key created — store this value now, it cannot be retrieved again", response, correlationId(httpRequest)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revoke(@PathVariable UUID id, HttpServletRequest httpRequest) {
        apiKeyService.revoke(id);
        return ApiResponse.message("API key revoked", correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
