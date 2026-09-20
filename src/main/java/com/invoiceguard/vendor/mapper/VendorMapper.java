package com.invoiceguard.vendor.mapper;

import com.invoiceguard.vendor.dto.VendorResponse;
import com.invoiceguard.vendor.entity.Vendor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    VendorResponse toResponse(Vendor vendor);
}
