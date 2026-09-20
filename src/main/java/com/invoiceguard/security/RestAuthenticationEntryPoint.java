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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Replaces Spring Security's default HTML/basic-auth challenge with the
 * project's standard JSON {@link ErrorResponse} shape, for any request that
 * reaches a protected endpoint without a valid authentication.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.AUTHENTICATION_FAILED.name(),
                "Authentication is required to access this resource",
                request.getRequestURI(),
                correlationId);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
