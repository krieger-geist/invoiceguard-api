package com.invoiceguard.security;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Tracks revoked access-token IDs (JWT {@code jti} claims) in Redis so a
 * token can be invalidated before its natural expiry — the one thing a
 * stateless JWT can't do on its own. Used on logout (see
 * {@code AuthController.logout}) to kill the specific access token that was
 * used to call it.
 *
 * <p>Entries are stored with a TTL equal to the token's own remaining
 * lifetime — once a token would have expired naturally anyway, there's no
 * reason to keep its blacklist entry around, so Redis cleans these up for free.
 *
 * <p>Fails open on Redis errors, same reasoning as {@link RateLimitingFilter}:
 * if Redis is down, tokens are validated purely on signature+expiry as
 * normal, rather than blocking every authenticated request in the app.
 */
@Component
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "blacklist:jti:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String jti, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
        } catch (Exception e) {
            log.warn("Could not write token to Redis blacklist (token will still expire naturally): {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (Exception e) {
            log.warn("Could not check Redis blacklist, treating token as not blacklisted: {}", e.getMessage());
            return false;
        }
    }
}
