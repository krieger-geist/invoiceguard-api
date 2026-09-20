package com.invoiceguard.vendor.mapper;

import com.invoiceguard.vendor.dto.VendorBankAccountResponse;
import com.invoiceguard.vendor.entity.VendorBankAccount;
import org.mapstruct.Mapper;

/**
 * {@code encryptedAccountNumber} is deliberately excluded — it isn't a field
 * on {@link VendorBankAccountResponse} at all, so MapStruct never has a
 * chance to copy it into an API response.
 */
@Mapper(componentModel = "spring")
public interface VendorBankAccountMapper {

    VendorBankAccountResponse toResponse(VendorBankAccount account);
}
