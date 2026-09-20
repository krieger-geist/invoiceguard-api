package com.invoiceguard.user.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.user.dto.UserResponse;
import com.invoiceguard.user.entity.User;
import com.invoiceguard.user.mapper.UserMapper;
import com.invoiceguard.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")


@Tag(name = "Users", description = "Current user profile.")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final TenantContext tenantContext;

    public UserController(UserService userService, UserMapper userMapper, TenantContext tenantContext) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(HttpServletRequest httpRequest) {
        User user = userService.getById(tenantContext.requireUserId());
        Object correlationId = httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return ApiResponse.ok(userMapper.toResponse(user), String.valueOf(correlationId));
    }
}
