package com.invoiceguard.invoice.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.common.response.PageResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.invoice.dto.InvoiceCreateRequest;
import com.invoiceguard.invoice.dto.InvoiceItemResponse;
import com.invoiceguard.invoice.dto.InvoiceResponse;
import com.invoiceguard.invoice.dto.InvoiceStatusHistoryResponse;
import com.invoiceguard.invoice.dto.InvoiceUpdateRequest;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.invoice.entity.InvoiceStatus;
import com.invoiceguard.invoice.mapper.InvoiceItemMapper;
import com.invoiceguard.invoice.mapper.InvoiceMapper;
import com.invoiceguard.invoice.mapper.InvoiceStatusHistoryMapper;
import com.invoiceguard.invoice.service.InvoiceService;
import com.invoiceguard.invoice.service.InvoiceService.InvoiceCreationOutcome;
import com.invoiceguard.invoice.service.InvoiceService.InvoiceSearchFilter;
import com.invoiceguard.risk.dto.RiskAssessmentResponse;
import com.invoiceguard.risk.entity.RiskAssessment;
import com.invoiceguard.risk.mapper.RiskAssessmentMapper;
import com.invoiceguard.risk.service.RiskAssessmentQueryService;
import com.invoiceguard.risk.service.RiskEngine;
import com.invoiceguard.approval.dto.ApprovalDecisionRequest;
import com.invoiceguard.approval.dto.ApprovalRequestResponse;
import com.invoiceguard.approval.entity.ApprovalRequest;
import com.invoiceguard.approval.mapper.ApprovalMapper;
import com.invoiceguard.approval.service.ApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/invoices")


@Tag(name = "Invoices", description = "Invoice CRUD, search, idempotent creation (Idempotency-Key header), status transitions, risk analysis, and approval actions.")
public class InvoiceController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceItemMapper invoiceItemMapper;
    private final InvoiceStatusHistoryMapper statusHistoryMapper;
    private final RiskEngine riskEngine;
    private final RiskAssessmentQueryService riskAssessmentQueryService;
    private final RiskAssessmentMapper riskAssessmentMapper;
    private final ApprovalService approvalService;
    private final ApprovalMapper approvalMapper;

    public InvoiceController(
            InvoiceService invoiceService,
            InvoiceMapper invoiceMapper,
            InvoiceItemMapper invoiceItemMapper,
            InvoiceStatusHistoryMapper statusHistoryMapper,
            RiskEngine riskEngine,
            RiskAssessmentQueryService riskAssessmentQueryService,
            RiskAssessmentMapper riskAssessmentMapper,
            ApprovalService approvalService,
            ApprovalMapper approvalMapper) {
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
        this.invoiceItemMapper = invoiceItemMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.riskEngine = riskEngine;
        this.riskAssessmentQueryService = riskAssessmentQueryService;
        this.riskAssessmentMapper = riskAssessmentMapper;
        this.approvalService = approvalService;
        this.approvalMapper = approvalMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('invoice:read')")
    public ApiResponse<PageResponse<InvoiceResponse>> search(
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) InvoiceRiskLevel riskLevel,
            @RequestParam(required = false) String purchaseOrderNumber,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        InvoiceSearchFilter filter = new InvoiceSearchFilter(
                vendorId, status, riskLevel, purchaseOrderNumber, createdBy, minAmount, maxAmount,
                invoiceDateFrom, invoiceDateTo, dueDateFrom, dueDateTo, keyword);
        Page<Invoice> page = invoiceService.search(filter, capPageSize(pageable));
        Page<InvoiceResponse> mapped = page.map(invoice -> toResponse(invoice, false));
        return ApiResponse.ok(PageResponse.from(mapped), correlationId(httpRequest));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('invoice:write')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(
            @Valid @RequestBody InvoiceCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        InvoiceCreationOutcome outcome = invoiceService.create(request, idempotencyKey);
        String message = outcome.replayed()
                ? "Returned previously created invoice for this idempotency key"
                : "Invoice created";
        return ResponseEntity.status(outcome.httpStatus())
                .body(ApiResponse.of(message, toResponse(outcome.invoice(), true), correlationId(httpRequest)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('invoice:read')")
    public ApiResponse<InvoiceResponse> getById(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Invoice invoice = invoiceService.getOwnedById(id);
        return ApiResponse.ok(toResponse(invoice, true), correlationId(httpRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('invoice:write')")
    public ApiResponse<InvoiceResponse> update(
            @PathVariable UUID id, @Valid @RequestBody InvoiceUpdateRequest request, HttpServletRequest httpRequest) {
        Invoice invoice = invoiceService.update(id, request);
        return ApiResponse.of("Invoice updated", toResponse(invoice, true), correlationId(httpRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('invoice:write')")
    public ApiResponse<Void> delete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        invoiceService.softDelete(id);
        return ApiResponse.message("Invoice deleted", correlationId(httpRequest));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('invoice:write')")
    public ApiResponse<InvoiceResponse> submit(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Invoice invoice = invoiceService.submit(id);
        return ApiResponse.of("Invoice submitted for analysis", toResponse(invoice, true), correlationId(httpRequest));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('invoice:read')")
    public ApiResponse<List<InvoiceStatusHistoryResponse>> history(@PathVariable UUID id, HttpServletRequest httpRequest) {
        List<InvoiceStatusHistoryResponse> history =
                invoiceService.getHistory(id).stream().map(statusHistoryMapper::toResponse).toList();
        return ApiResponse.ok(history, correlationId(httpRequest));
    }

    @PostMapping("/{id}/analyse")
    @PreAuthorize("hasAuthority('invoice:write')")
    public ApiResponse<RiskAssessmentResponse> analyse(@PathVariable UUID id, HttpServletRequest httpRequest) {
        RiskAssessment assessment = riskEngine.analyse(id);
        List<com.invoiceguard.risk.entity.RiskFinding> findings = riskAssessmentQueryService.getFindings(assessment.getId());
        return ApiResponse.of(
                "Risk analysis completed",
                riskAssessmentMapper.toResponse(assessment, findings),
                correlationId(httpRequest));
    }

    @GetMapping("/{id}/risk-assessment")
    @PreAuthorize("hasAuthority('invoice:read')")
    public ApiResponse<RiskAssessmentResponse> riskAssessment(@PathVariable UUID id, HttpServletRequest httpRequest) {
        invoiceService.getOwnedById(id); // tenant + existence check
        RiskAssessment assessment = riskAssessmentQueryService.getLatestForInvoice(id);
        List<com.invoiceguard.risk.entity.RiskFinding> findings = riskAssessmentQueryService.getFindings(assessment.getId());
        return ApiResponse.ok(riskAssessmentMapper.toResponse(assessment, findings), correlationId(httpRequest));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('invoice:approve')")
    public ApiResponse<ApprovalRequestResponse> approve(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ApprovalDecisionRequest request, HttpServletRequest httpRequest) {
        String comments = request != null ? request.comments() : null;
        ApprovalRequest approvalRequest = approvalService.approve(id, comments);
        return ApiResponse.of(
                "Approval recorded",
                approvalMapper.toResponse(approvalRequest, approvalService.getSteps(approvalRequest.getId())),
                correlationId(httpRequest));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('invoice:approve')")
    public ApiResponse<ApprovalRequestResponse> reject(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ApprovalDecisionRequest request, HttpServletRequest httpRequest) {
        String comments = request != null ? request.comments() : null;
        ApprovalRequest approvalRequest = approvalService.reject(id, comments);
        return ApiResponse.of(
                "Invoice rejected",
                approvalMapper.toResponse(approvalRequest, approvalService.getSteps(approvalRequest.getId())),
                correlationId(httpRequest));
    }

    @GetMapping("/{id}/approval")
    @PreAuthorize("hasAuthority('invoice:read')")
    public ApiResponse<ApprovalRequestResponse> approvalStatus(@PathVariable UUID id, HttpServletRequest httpRequest) {
        ApprovalRequest approvalRequest = approvalService.getForInvoice(id);
        return ApiResponse.ok(
                approvalMapper.toResponse(approvalRequest, approvalService.getSteps(approvalRequest.getId())),
                correlationId(httpRequest));
    }

    private InvoiceResponse toResponse(Invoice invoice, boolean includeItems) {
        List<InvoiceItemResponse> items = includeItems
                ? invoiceService.getItems(invoice.getId()).stream().map(invoiceItemMapper::toResponse).toList()
                : List.of();
        return invoiceMapper.toResponseWithItems(invoice, items);
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
