package com.invoiceguard.invoice.mapper;

import com.invoiceguard.invoice.dto.InvoiceItemResponse;
import com.invoiceguard.invoice.entity.InvoiceItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceItemMapper {

    InvoiceItemResponse toResponse(InvoiceItem item);
}
