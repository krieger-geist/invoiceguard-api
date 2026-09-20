package com.invoiceguard.vendor.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.common.response.PageResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.vendor.dto.VendorCreateRequest;
import com.invoiceguard.vendor.dto.VendorResponse;
import com.invoiceguard.vendor.dto.VendorUpdateRequest;
import com.invoiceguard.vendor.dto.VendorVerificationDecisionRequest;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorRiskStatus;
import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import com.invoiceguard.vendor.mapper.VendorMapper;
import com.invoiceguard.vendor.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/vendors")


@Tag(name = "Vendors", description = "Vendor CRUD, search, and identity verification workflow.")
public class VendorController {

    private static final int MAX_PAGE_SIZE = 100;

    private final VendorService vendorService;
    private final VendorMapper vendorMapper;

    public VendorController(VendorService vendorService, VendorMapper vendorMapper) {
        this.vendorService = vendorService;
        this.vendorMapper = vendorMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('vendor:read')")
    public ApiResponse<PageResponse<VendorResponse>> search(
            @RequestParam(required = false) VendorVerificationStatus verificationStatus,
            @RequestParam(required = false) VendorRiskStatus riskStatus,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        Pageable safePageable = capPageSize(pageable);
        Page<Vendor> page =
                vendorService.search(verificationStatus, riskStatus, active, country, keyword, safePageable);
        Page<VendorResponse> mapped = page.map(vendorMapper::toResponse);
        return ApiResponse.ok(PageResponse.from(mapped), correlationId(httpRequest));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('vendor:write')")
    public ResponseEntity<ApiResponse<VendorResponse>> create(
            @Valid @RequestBody VendorCreateRequest request, HttpServletRequest httpRequest) {
        Vendor vendor = vendorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Vendor created", vendorMapper.toResponse(vendor), correlationId(httpRequest)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('vendor:read')")
    public ApiResponse<VendorResponse> getById(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Vendor vendor = vendorService.getOwnedById(id);
        return ApiResponse.ok(vendorMapper.toResponse(vendor), correlationId(httpRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('vendor:write')")
    public ApiResponse<VendorResponse> update(
            @PathVariable UUID id, @Valid @RequestBody VendorUpdateRequest request, HttpServletRequest httpRequest) {
        Vendor vendor = vendorService.update(id, request);
        return ApiResponse.of("Vendor updated", vendorMapper.toResponse(vendor), correlationId(httpRequest));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('vendor:verify')")
    public ApiResponse<VendorResponse> verify(
            @PathVariable UUID id,
            @Valid @RequestBody VendorVerificationDecisionRequest request,
            HttpServletRequest httpRequest) {
        Vendor vendor = vendorService.changeVerificationStatus(id, request.targetStatus());
        return ApiResponse.of(
                "Vendor verification status updated to " + request.targetStatus(),
                vendorMapper.toResponse(vendor),
                correlationId(httpRequest));
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
