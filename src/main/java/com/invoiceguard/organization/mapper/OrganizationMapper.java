package com.invoiceguard.organization.mapper;

import com.invoiceguard.organization.dto.OrganizationResponse;
import com.invoiceguard.organization.entity.Organization;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationResponse toResponse(Organization organization);
}
