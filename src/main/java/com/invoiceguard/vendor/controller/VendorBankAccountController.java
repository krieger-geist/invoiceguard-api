package com.invoiceguard.vendor.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.vendor.dto.BankAccountSubmissionRequest;
import com.invoiceguard.vendor.dto.BankChangeDecisionRequest;
import com.invoiceguard.vendor.dto.BankChangeRequestResponse;
import com.invoiceguard.vendor.dto.VendorBankAccountResponse;
import com.invoiceguard.vendor.entity.BankChangeRequest;
import com.invoiceguard.vendor.entity.VendorBankAccount;
import com.invoiceguard.vendor.mapper.BankChangeRequestMapper;
import com.invoiceguard.vendor.mapper.VendorBankAccountMapper;
import com.invoiceguard.vendor.service.VendorBankAccountService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/v1/vendors/{vendorId}")

@Tag(name = "Vendor Bank Accounts", description = "Bank-account history and the approval-required bank-change-request workflow.")
public class VendorBankAccountController {

    private final VendorBankAccountService bankAccountService;
    private final VendorBankAccountMapper bankAccountMapper;
    private final BankChangeRequestMapper bankChangeRequestMapper;
    private final TenantContext tenantContext;

    public VendorBankAccountController(
            VendorBankAccountService bankAccountService,
            VendorBankAccountMapper bankAccountMapper,
            BankChangeRequestMapper bankChangeRequestMapper,
            TenantContext tenantContext) {
        this.bankAccountService = bankAccountService;
        this.bankAccountMapper = bankAccountMapper;
        this.bankChangeRequestMapper = bankChangeRequestMapper;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/bank-accounts")
    @PreAuthorize("hasAuthority('vendor:read')")
    public ApiResponse<List<VendorBankAccountResponse>> listBankAccounts(
            @PathVariable UUID vendorId, HttpServletRequest httpRequest) {
        List<VendorBankAccount> accounts = bankAccountService.listForVendor(vendorId);
        List<VendorBankAccountResponse> response = accounts.stream().map(bankAccountMapper::toResponse).toList();
        return ApiResponse.ok(response, correlationId(httpRequest));
    }

    @GetMapping("/bank-change-requests")
    @PreAuthorize("hasAuthority('vendor:read')")
    public ApiResponse<List<BankChangeRequestResponse>> listChangeRequests(
            @PathVariable UUID vendorId, HttpServletRequest httpRequest) {
        List<BankChangeRequest> requests = bankAccountService.listChangeRequestsForVendor(vendorId);
        List<BankChangeRequestResponse> response = requests.stream().map(bankChangeRequestMapper::toResponse).toList();
        return ApiResponse.ok(response, correlationId(httpRequest));
    }

    @PostMapping("/bank-change-requests")
    @PreAuthorize("hasAuthority('vendor:write')")
    public ResponseEntity<ApiResponse<BankChangeRequestResponse>> submit(
            @PathVariable UUID vendorId,
            @Valid @RequestBody BankAccountSubmissionRequest request,
            HttpServletRequest httpRequest) {
        BankChangeRequest changeRequest =
                bankAccountService.submitBankAccount(vendorId, request, tenantContext.requireUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        "Bank change request submitted and is pending approval",
                        bankChangeRequestMapper.toResponse(changeRequest),
                        correlationId(httpRequest)));
    }

    @PostMapping("/bank-change-requests/{requestId}/decision")
    @PreAuthorize("hasAuthority('vendor:bank-change-approve')")
    public ApiResponse<BankChangeRequestResponse> decide(
            @PathVariable UUID vendorId,
            @PathVariable UUID requestId,
            @Valid @RequestBody BankChangeDecisionRequest request,
            HttpServletRequest httpRequest) {
        BankChangeRequest changeRequest = bankAccountService.decide(
                vendorId, requestId, request.approve(), request.notes(), tenantContext.requireUserId());
        String message = request.approve() ? "Bank change request approved" : "Bank change request rejected";
        return ApiResponse.of(message, bankChangeRequestMapper.toResponse(changeRequest), correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
