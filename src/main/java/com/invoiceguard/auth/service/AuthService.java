package com.invoiceguard.auth.service;

import com.invoiceguard.auth.dto.AuthTokenResponse;
import com.invoiceguard.auth.dto.LoginRequest;
import com.invoiceguard.auth.dto.RegisterRequest;
import com.invoiceguard.auth.entity.EmailVerificationToken;
import com.invoiceguard.auth.entity.PasswordResetToken;
import com.invoiceguard.auth.entity.RefreshToken;
import com.invoiceguard.auth.event.AccountLockedEvent;
import com.invoiceguard.auth.event.LoginFailedEvent;
import com.invoiceguard.auth.event.LoginSucceededEvent;
import com.invoiceguard.auth.event.UserRegisteredEvent;
import com.invoiceguard.auth.repository.EmailVerificationTokenRepository;
import com.invoiceguard.auth.repository.PasswordResetTokenRepository;
import com.invoiceguard.auth.repository.RefreshTokenRepository;
import com.invoiceguard.exception.ApplicationException;
import com.invoiceguard.exception.DuplicateResourceException;
import com.invoiceguard.exception.ErrorCode;
import com.invoiceguard.notification.EmailService;
import com.invoiceguard.organization.entity.Organization;
import com.invoiceguard.organization.entity.OrganizationMember;
import com.invoiceguard.organization.entity.OrganizationMemberStatus;
import com.invoiceguard.organization.repository.OrganizationMemberRepository;
import com.invoiceguard.organization.service.OrganizationMemberService;
import com.invoiceguard.organization.service.OrganizationService;
import com.invoiceguard.security.AuthenticatedPrincipal;
import com.invoiceguard.security.JwtTokenProvider;
import com.invoiceguard.security.OpaqueTokenService;
import com.invoiceguard.security.Role;
import com.invoiceguard.security.RolePermissions;
import com.invoiceguard.security.SecurityProperties;
import com.invoiceguard.user.entity.User;
import com.invoiceguard.user.entity.UserStatus;
import com.invoiceguard.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int PASSWORD_RESET_TTL_MINUTES = 30;
    private static final int EMAIL_VERIFICATION_TTL_HOURS = 48;

    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OrganizationService organizationService;
    private final OrganizationMemberService organizationMemberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OpaqueTokenService opaqueTokenService;
    private final SecurityProperties securityProperties;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(
            UserRepository userRepository,
            OrganizationMemberRepository organizationMemberRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            OrganizationService organizationService,
            OrganizationMemberService organizationMemberService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            OpaqueTokenService opaqueTokenService,
            SecurityProperties securityProperties,
            EmailService emailService,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.organizationService = organizationService;
        this.organizationMemberService = organizationMemberService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.opaqueTokenService = opaqueTokenService;
        this.securityProperties = securityProperties;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    // ---- Registration --------------------------------------------------------------

    @Transactional
    public AuthTokenResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        Organization organization = organizationService.create(request.organizationName(), null);

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user = userRepository.save(user);

        organizationMemberService.addExistingUserAsAdmin(organization.getId(), user.getId());

        issueEmailVerificationToken(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), organization.getId(), user.getEmail()));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                user.getId(), user.getEmail(), organization.getId(), Role.ORGANIZATION_ADMIN,
                RolePermissions.permissionsFor(Role.ORGANIZATION_ADMIN));

        return issueTokenPair(principal, ipAddress, userAgent);
    }

    // ---- Login ----------------------------------------------------------------------

    @Transactional
    public AuthTokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository
                .findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> failLogin(request.email(), ipAddress, "No such account"));

        if (user.isCurrentlyLocked()) {
            eventPublisher.publishEvent(new LoginFailedEvent(request.email(), ipAddress, "Account locked"));
            throw new ApplicationException(ErrorCode.ACCOUNT_LOCKED, "Account is temporarily locked. Try again later.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            eventPublisher.publishEvent(new LoginFailedEvent(request.email(), ipAddress, "Account not active"));
            throw new ApplicationException(ErrorCode.AUTHENTICATION_FAILED, "Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw failLogin(request.email(), ipAddress, "Bad credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        OrganizationMember membership = resolveMembership(user.getId(), request.organizationId());

        eventPublisher.publishEvent(new LoginSucceededEvent(user.getId(), membership.getOrganizationId(), ipAddress, userAgent));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                user.getId(),
                user.getEmail(),
                membership.getOrganizationId(),
                membership.getRole(),
                RolePermissions.permissionsFor(membership.getRole()));

        return issueTokenPair(principal, ipAddress, userAgent);
    }

    private OrganizationMember resolveMembership(UUID userId, UUID requestedOrganizationId) {
        List<OrganizationMember> memberships = organizationMemberRepository.findByUserId(userId).stream()
                .filter(m -> m.getStatus() == OrganizationMemberStatus.ACTIVE)
                .toList();

        if (memberships.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.AUTHENTICATION_FAILED, "This account has no active organisation membership");
        }

        if (requestedOrganizationId != null) {
            return memberships.stream()
                    .filter(m -> m.getOrganizationId().equals(requestedOrganizationId))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException(
                            ErrorCode.AUTHENTICATION_FAILED, "No active membership in the requested organisation"));
        }

        if (memberships.size() == 1) {
            return memberships.get(0);
        }

        throw new ApplicationException(
                ErrorCode.ORGANIZATION_CONTEXT_REQUIRED,
                "This account belongs to multiple organisations; specify organizationId when logging in");
    }

    private void registerFailedAttempt(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= securityProperties.maxFailedLoginAttempts()) {
            user.setLockedUntil(Instant.now().plus(securityProperties.accountLockMinutes(), ChronoUnit.MINUTES));
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            eventPublisher.publishEvent(new AccountLockedEvent(user.getId(), user.getEmail(), null));
            return;
        }
        userRepository.save(user);
    }

    private ApplicationException failLogin(String email, String ipAddress, String reason) {
        eventPublisher.publishEvent(new LoginFailedEvent(email, ipAddress, reason));
        return new ApplicationException(ErrorCode.AUTHENTICATION_FAILED, "Invalid email or password");
    }

    // ---- Refresh / Logout ------------------------------------------------------------

    @Transactional
    public AuthTokenResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        String hash = opaqueTokenService.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_TOKEN, "Invalid refresh token"));

        if (existing.isRevoked()) {
            // Reuse of an already-rotated/revoked token: treat as compromise and kill every session.
            refreshTokenRepository.revokeAllForUser(existing.getUserId());
            throw new ApplicationException(
                    ErrorCode.INVALID_TOKEN, "Refresh token has already been used; all sessions have been revoked");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ApplicationException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired");
        }

        User user = userRepository
                .findById(existing.getUserId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTHENTICATION_FAILED, "Account no longer exists"));

        OrganizationMember membership = organizationMemberRepository
                .findByUserIdAndOrganizationId(user.getId(), existing.getOrganizationId())
                .filter(m -> m.getStatus() == OrganizationMemberStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.AUTHENTICATION_FAILED, "No active membership in this organisation"));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                user.getId(),
                user.getEmail(),
                membership.getOrganizationId(),
                membership.getRole(),
                RolePermissions.permissionsFor(membership.getRole()));

        AuthTokenResponse newTokens = issueTokenPair(principal, ipAddress, userAgent);

        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);

        return newTokens;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = opaqueTokenService.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    private AuthTokenResponse issueTokenPair(AuthenticatedPrincipal principal, String ipAddress, String userAgent) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);

        String rawRefreshToken = opaqueTokenService.generateToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(principal.userId());
        refreshToken.setOrganizationId(principal.organizationId());
        refreshToken.setTokenHash(opaqueTokenService.hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshTokenRepository.save(refreshToken);

        return AuthTokenResponse.bearer(
                accessToken,
                rawRefreshToken,
                jwtTokenProvider.accessTokenTtlMinutes() * 60,
                principal.userId(),
                principal.organizationId(),
                principal.role().name());
    }

    // ---- Password reset ---------------------------------------------------------------

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = opaqueTokenService.generateToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(opaqueTokenService.hash(rawToken));
            token.setExpiresAt(Instant.now().plus(PASSWORD_RESET_TTL_MINUTES, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);
            emailService.sendPasswordReset(user.getEmail(), user.fullName(), rawToken);
        });
        // Always returns normally regardless of whether the email existed, to avoid account enumeration.
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        String hash = opaqueTokenService.hash(rawToken);
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(hash)
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(
                        () -> new ApplicationException(ErrorCode.INVALID_TOKEN, "Invalid or expired reset token"));

        User user = userRepository
                .findById(token.getUserId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND, "User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Force re-authentication everywhere — a password reset should invalidate old sessions.
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    // ---- Email verification -------------------------------------------------------------

    @Transactional
    public void resendEmailVerification(String email) {
        userRepository
                .findByEmailIgnoreCase(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueEmailVerificationToken);
    }

    @Transactional
    public void confirmEmailVerification(String rawToken) {
        String hash = opaqueTokenService.hash(rawToken);
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHash(hash)
                .filter(EmailVerificationToken::isUsable)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.INVALID_TOKEN, "Invalid or expired verification token"));

        User user = userRepository
                .findById(token.getUserId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND, "User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        emailVerificationTokenRepository.save(token);
    }

    private void issueEmailVerificationToken(User user) {
        String rawToken = opaqueTokenService.generateToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setTokenHash(opaqueTokenService.hash(rawToken));
        token.setExpiresAt(Instant.now().plus(EMAIL_VERIFICATION_TTL_HOURS, ChronoUnit.HOURS));
        emailVerificationTokenRepository.save(token);
        emailService.sendEmailVerification(user.getEmail(), user.fullName(), rawToken);
    }
}
