package com.invoiceguard.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = Mockito.mock(TokenBlacklistService.class);
        Mockito.when(tokenBlacklistService.isBlacklisted(Mockito.anyString())).thenReturn(false);

        JwtProperties properties = new JwtProperties(
                "test-secret-key-for-unit-tests-only-0123456789ABCDEF", 15, 7, "invoiceguard-test");
        jwtTokenProvider = new JwtTokenProvider(properties, tokenBlacklistService);
    }

    private AuthenticatedPrincipal samplePrincipal() {
        return new AuthenticatedPrincipal(
                UUID.randomUUID(), "user@example.com", UUID.randomUUID(), Role.FINANCE_MANAGER,
                Set.of(Permission.INVOICE_READ, Permission.INVOICE_APPROVE));
    }

    @Test
    void tokenRoundTripsBackToTheSamePrincipalData() {
        AuthenticatedPrincipal original = samplePrincipal();
        String token = jwtTokenProvider.generateAccessToken(original);

        var parsed = jwtTokenProvider.parseAndValidate(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(original.userId());
        assertThat(parsed.get().organizationId()).isEqualTo(original.organizationId());
        assertThat(parsed.get().role()).isEqualTo(Role.FINANCE_MANAGER);
        assertThat(parsed.get().permissions()).containsExactlyInAnyOrder(Permission.INVOICE_READ, Permission.INVOICE_APPROVE);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtTokenProvider.generateAccessToken(samplePrincipal());
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtTokenProvider.parseAndValidate(tampered)).isEmpty();
    }

    @Test
    void rejectsCompletelyMalformedToken() {
        assertThat(jwtTokenProvider.parseAndValidate("not-a-jwt-at-all")).isEmpty();
    }

    @Test
    void rejectsBlacklistedToken() {
        Mockito.when(tokenBlacklistService.isBlacklisted(Mockito.anyString())).thenReturn(true);
        String token = jwtTokenProvider.generateAccessToken(samplePrincipal());

        assertThat(jwtTokenProvider.parseAndValidate(token)).isEmpty();
    }

    @Test
    void refusesToStartWithoutASecret() {
        JwtProperties blankSecretProperties = new JwtProperties("", 15, 7, "invoiceguard-test");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtTokenProvider(blankSecretProperties, tokenBlacklistService))
                .isInstanceOf(IllegalStateException.class);
    }
}
