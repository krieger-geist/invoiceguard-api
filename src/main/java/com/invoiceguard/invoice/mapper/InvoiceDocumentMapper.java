package com.invoiceguard.invoice.mapper;

import com.invoiceguard.invoice.dto.InvoiceDocumentResponse;
import com.invoiceguard.invoice.entity.InvoiceDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceDocumentMapper {

    InvoiceDocumentResponse toResponse(InvoiceDocument document);
}
