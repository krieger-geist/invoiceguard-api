package com.invoiceguard.alert.mapper;

import com.invoiceguard.alert.dto.AlertResponse;
import com.invoiceguard.alert.entity.Alert;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    AlertResponse toResponse(Alert alert);
}
