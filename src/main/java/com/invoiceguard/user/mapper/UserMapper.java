package com.invoiceguard.user.mapper;

import com.invoiceguard.user.dto.UserResponse;
import com.invoiceguard.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
