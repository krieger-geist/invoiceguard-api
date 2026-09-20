package com.invoiceguard.alert.controller;

import com.invoiceguard.alert.dto.AlertResponse;
import com.invoiceguard.alert.entity.Alert;
import com.invoiceguard.alert.entity.AlertStatus;
import com.invoiceguard.alert.entity.AlertType;
import com.invoiceguard.alert.mapper.AlertMapper;
import com.invoiceguard.alert.service.AlertService;
import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.common.response.PageResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Alerts", description = "In-app alerts raised automatically by domain events (critical risk, duplicate invoices, vendor changes, ...).")
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;
    private final AlertMapper alertMapper;

    public AlertController(AlertService alertService, AlertMapper alertMapper) {
        this.alertService = alertService;
        this.alertMapper = alertMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('alert:read')")
    public ApiResponse<PageResponse<AlertResponse>> list(
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        Page<Alert> page = alertService.search(type, status, pageable);
        Page<AlertResponse> mapped = page.map(alertMapper::toResponse);
        return ApiResponse.ok(PageResponse.from(mapped), correlationId(httpRequest));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('alert:manage')")
    public ApiResponse<AlertResponse> acknowledge(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Alert alert = alertService.acknowledge(id);
        return ApiResponse.of("Alert acknowledged", alertMapper.toResponse(alert), correlationId(httpRequest));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('alert:manage')")
    public ApiResponse<AlertResponse> resolve(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Alert alert = alertService.resolve(id);
        return ApiResponse.of("Alert resolved", alertMapper.toResponse(alert), correlationId(httpRequest));
    }

    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasAuthority('alert:manage')")
    public ApiResponse<AlertResponse> dismiss(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Alert alert = alertService.dismiss(id);
        return ApiResponse.of("Alert dismissed", alertMapper.toResponse(alert), correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}