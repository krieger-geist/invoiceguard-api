package com.invoiceguard.invoice.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.invoice.dto.InvoiceDocumentResponse;
import com.invoiceguard.invoice.entity.InvoiceDocument;
import com.invoiceguard.invoice.mapper.InvoiceDocumentMapper;
import com.invoiceguard.invoice.service.InvoiceDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/documents")


@Tag(name = "Invoice Documents", description = "Document metadata upload/listing for invoices.")
public class InvoiceDocumentController {

    private final InvoiceDocumentService documentService;
    private final InvoiceDocumentMapper documentMapper;

    public InvoiceDocumentController(InvoiceDocumentService documentService, InvoiceDocumentMapper documentMapper) {
        this.documentService = documentService;
        this.documentMapper = documentMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('invoice:write')")
    public ResponseEntity<ApiResponse<InvoiceDocumentResponse>> upload(
            @PathVariable UUID invoiceId, @RequestParam("file") MultipartFile file, HttpServletRequest httpRequest) {
        try {
            InvoiceDocument document = documentService.upload(
                    invoiceId,
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed",
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    file.getSize(),
                    file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.of(
                            "Document uploaded", documentMapper.toResponse(document), correlationId(httpRequest)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('invoice:read')")
    public ApiResponse<List<InvoiceDocumentResponse>> list(@PathVariable UUID invoiceId, HttpServletRequest httpRequest) {
        List<InvoiceDocumentResponse> documents =
                documentService.list(invoiceId).stream().map(documentMapper::toResponse).toList();
        return ApiResponse.ok(documents, correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
