package com.invoiceguard.auth.controller;

import com.invoiceguard.auth.dto.AuthTokenResponse;
import com.invoiceguard.auth.dto.EmailVerificationConfirmDto;
import com.invoiceguard.auth.dto.LoginRequest;
import com.invoiceguard.auth.dto.LogoutRequest;
import com.invoiceguard.auth.dto.PasswordResetConfirmDto;
import com.invoiceguard.auth.dto.PasswordResetRequestDto;
import com.invoiceguard.auth.dto.RefreshRequest;
import com.invoiceguard.auth.dto.RegisterRequest;
import com.invoiceguard.auth.dto.ResendVerificationRequest;
import com.invoiceguard.auth.service.AuthService;
import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * All endpoints here are public (see {@code SecurityConfig.PUBLIC_ENDPOINTS}) —
 * they are how a caller obtains the credentials everything else requires.
 */
@RestController
@RequestMapping("/api/v1/auth")


@Tag(name = "Auth", description = "Registration, login, token refresh/logout, password reset, email verification. All endpoints here are public.")
public class AuthController {

    private final AuthService authService;
    private final com.invoiceguard.security.JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, com.invoiceguard.security.JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthTokenResponse tokens = authService.register(request, clientIp(httpRequest), userAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Organisation and account created successfully", tokens, correlationId(httpRequest)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthTokenResponse tokens = authService.login(request, clientIp(httpRequest), userAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.of("Login successful", tokens, correlationId(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        AuthTokenResponse tokens =
                authService.refresh(request.refreshToken(), clientIp(httpRequest), userAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.of("Token refreshed", tokens, correlationId(httpRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        authService.logout(request.refreshToken());
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtTokenProvider.blacklistToken(authHeader.substring("Bearer ".length()));
        }
        return ResponseEntity.ok(ApiResponse.message("Logged out", correlationId(httpRequest)));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request, HttpServletRequest httpRequest) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.message(
                "If an account with that email exists, a password reset link has been sent",
                correlationId(httpRequest)));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDto request, HttpServletRequest httpRequest) {
        authService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.message("Password has been reset", correlationId(httpRequest)));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<ApiResponse<Void>> resendEmailVerification(
            @Valid @RequestBody ResendVerificationRequest request, HttpServletRequest httpRequest) {
        authService.resendEmailVerification(request.email());
        return ResponseEntity.ok(ApiResponse.message(
                "If an account with that email exists, a verification link has been sent",
                correlationId(httpRequest)));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmDto request, HttpServletRequest httpRequest) {
        authService.confirmEmailVerification(request.token());
        return ResponseEntity.ok(ApiResponse.message("Email verified", correlationId(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
