package com.invoiceguard.invoice.mapper;

import com.invoiceguard.invoice.dto.InvoiceItemResponse;
import com.invoiceguard.invoice.dto.InvoiceResponse;
import com.invoiceguard.invoice.entity.Invoice;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code items} isn't a field on {@link Invoice} (line items are a separate
 * table, loaded separately by {@code InvoiceService}) so it's excluded here
 * and assembled by the service/controller via {@link #toResponseWithItems}.
 */
@Mapper(componentModel = "spring", uses = InvoiceItemMapper.class)
public interface InvoiceMapper {

    @Mapping(target = "items", ignore = true)
    InvoiceResponse toResponseWithoutItems(Invoice invoice);

    default InvoiceResponse toResponseWithItems(Invoice invoice, List<InvoiceItemResponse> items) {
        InvoiceResponse base = toResponseWithoutItems(invoice);
        return new InvoiceResponse(
                base.id(),
                base.invoiceNumber(),
                base.vendorId(),
                base.invoiceDate(),
                base.dueDate(),
                base.currency(),
                base.subtotal(),
                base.taxAmount(),
                base.discountAmount(),
                base.totalAmount(),
                base.purchaseOrderNumber(),
                base.paymentReference(),
                base.description(),
                base.status(),
                base.riskLevel(),
                base.latestRiskScore(),
                items,
                base.createdAt(),
                base.updatedAt());
    }
}
