package com.invoiceguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceguard.common.response.ErrorResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;

/**
 * Fixed-window rate limiting via Redis {@code INCR}+{@code EXPIRE} — the
 * simplest correct approach for a per-minute limit: the first request in a
 * given minute-bucket creates the counter with a 60s TTL, every subsequent
 * request in that same window just increments it, and the bucket
 * self-expires with no cleanup job needed.
 *
 * <p>Rate-limit key is the authenticated caller (user ID or API key ID) once
 * {@link JwtAuthenticationFilter}/{@code ApiKeyAuthenticationFilter} have
 * run, falling back to remote IP for unauthenticated requests (e.g. repeated
 * login attempts) — this filter therefore runs AFTER both of those in the
 * chain, not before.
 *
 * <p>Fails open: if Redis itself is unreachable, the request is allowed
 * through rather than blocked — an outage in the rate limiter should not
 * become an outage of the whole API.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int defaultRequestsPerMinute;

    public RateLimitingFilter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${invoiceguard.rate-limit.default-requests-per-minute:120}") int defaultRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultRequestsPerMinute = defaultRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String identity = resolveIdentity(request);
        long window = Instant.now().getEpochSecond() / 60;
        String redisKey = KEY_PREFIX + identity + ":" + window;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(90));
            }
            if (count != null && count > defaultRequestsPerMinute) {
                respondTooManyRequests(request, response);
                return;
            }
        } catch (Exception e) {
            log.warn("Rate limiter could not reach Redis, allowing request through: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveIdentity(HttpServletRequest request) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return principal.userId().toString();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.RATE_LIMIT_EXCEEDED.name(), "Too many requests — please slow down", request.getRequestURI(), correlationId);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
