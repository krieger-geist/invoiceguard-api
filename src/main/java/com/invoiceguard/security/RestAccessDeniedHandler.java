package com.invoiceguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceguard.common.response.ErrorResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Replaces Spring Security's default 403 handling with the project's
 * standard JSON {@link ErrorResponse} shape — reached when an authenticated
 * caller lacks the role/permission required by {@code @PreAuthorize}.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.ACCESS_DENIED.name(),
                "You do not have permission to perform this action",
                request.getRequestURI(),
                correlationId);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
