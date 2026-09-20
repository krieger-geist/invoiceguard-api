package com.invoiceguard.invoice.mapper;

import com.invoiceguard.invoice.dto.InvoiceStatusHistoryResponse;
import com.invoiceguard.invoice.entity.InvoiceStatusHistory;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceStatusHistoryMapper {

    @Mapping(target = "changedBy", source = "createdBy")
    @Mapping(target = "changedAt", source = "createdAt")
    InvoiceStatusHistoryResponse toResponse(InvoiceStatusHistory history);
}
