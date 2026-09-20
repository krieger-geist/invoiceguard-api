package com.invoiceguard.organization.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.organization.dto.OrganizationResponse;
import com.invoiceguard.organization.entity.Organization;
import com.invoiceguard.organization.mapper.OrganizationMapper;
import com.invoiceguard.organization.service.OrganizationService;
import com.invoiceguard.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/organizations")


@Tag(name = "Organizations", description = "Current organisation details.")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationMapper organizationMapper;
    private final TenantContext tenantContext;

    public OrganizationController(
            OrganizationService organizationService, OrganizationMapper organizationMapper, TenantContext tenantContext) {
        this.organizationService = organizationService;
        this.organizationMapper = organizationMapper;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/me")
    public ApiResponse<OrganizationResponse> getCurrentOrganization(HttpServletRequest httpRequest) {
        Organization organization = organizationService.getById(tenantContext.requireOrganizationId());
        Object correlationId = httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return ApiResponse.ok(organizationMapper.toResponse(organization), String.valueOf(correlationId));
    }
}
