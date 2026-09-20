package com.invoiceguard.vendor.mapper;

import com.invoiceguard.vendor.dto.BankChangeRequestResponse;
import com.invoiceguard.vendor.entity.BankChangeRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BankChangeRequestMapper {

    BankChangeRequestResponse toResponse(BankChangeRequest request);
}
