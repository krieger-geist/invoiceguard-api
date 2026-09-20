package com.invoiceguard.approval.controller;

import com.invoiceguard.approval.dto.ApprovalPolicyRequest;
import com.invoiceguard.approval.dto.ApprovalPolicyResponse;
import com.invoiceguard.approval.service.ApprovalPolicyService;
import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Org-wide financial control configuration — restricted to {@code organization:manage} (org admins / super admins). */
@RestController
@RequestMapping("/api/v1/approval-policies")


@Tag(name = "Approval Policies", description = "Configurable amount-tier approval requirements.")
public class ApprovalPolicyController {

    private final ApprovalPolicyService service;

    public ApprovalPolicyController(ApprovalPolicyService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organization:manage')")
    public ApiResponse<List<ApprovalPolicyResponse>> list(HttpServletRequest httpRequest) {
        return ApiResponse.ok(service.listAll(), correlationId(httpRequest));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organization:manage')")
    public ResponseEntity<ApiResponse<ApprovalPolicyResponse>> create(
            @Valid @RequestBody ApprovalPolicyRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Approval policy created", service.create(request), correlationId(httpRequest)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('organization:manage')")
    public ApiResponse<ApprovalPolicyResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ApprovalPolicyRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.of("Approval policy updated", service.update(id, request), correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
