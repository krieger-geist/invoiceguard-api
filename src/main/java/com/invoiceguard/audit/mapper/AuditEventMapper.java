package com.invoiceguard.audit.mapper;

import com.invoiceguard.audit.dto.AuditEventResponse;
import com.invoiceguard.audit.entity.AuditEvent;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    @Mapping(target = "performedBy", source = "createdBy")
    @Mapping(target = "occurredAt", source = "createdAt")
    AuditEventResponse toResponse(AuditEvent event);
}
