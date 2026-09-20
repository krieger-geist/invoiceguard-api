package com.invoiceguard.audit.controller;

import com.invoiceguard.audit.dto.AuditEventResponse;
import com.invoiceguard.audit.entity.AuditEvent;
import com.invoiceguard.audit.mapper.AuditEventMapper;
import com.invoiceguard.audit.service.AuditService;
import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.common.response.PageResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Read-only by design — see {@code AuditEvent} javadoc for why no update/delete endpoint exists here. */
@RestController
@RequestMapping("/api/v1/audit-logs")


@Tag(name = "Audit Logs", description = "Read-only immutable audit trail.")
public class AuditController {

    private final AuditService auditService;
    private final AuditEventMapper mapper;

    public AuditController(AuditService auditService, AuditEventMapper mapper) {
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<PageResponse<AuditEventResponse>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        Page<AuditEvent> page = auditService.search(action, entityType, from, to, pageable);
        Page<AuditEventResponse> mapped = page.map(mapper::toResponse);
        Object correlationId = httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return ApiResponse.ok(PageResponse.from(mapped), String.valueOf(correlationId));
    }
}
