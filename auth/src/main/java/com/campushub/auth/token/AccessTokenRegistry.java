package com.campushub.auth.token;

import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class AccessTokenRegistry {

    private static final String SESSION_KEY_PREFIX = "auth:access:session:";
    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock;

    public AccessTokenRegistry(
            StringRedisTemplate stringRedisTemplate,
            Clock clock
    ) {
        this.stringRedisTemplate =
                Objects.requireNonNull(stringRedisTemplate);
        this.clock = Objects.requireNonNull(clock);
    }

    public void register(
            UUID accountId,
            UUID sessionId,
            UUID tokenId,
            Instant sessionExpiresAt
    ) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(tokenId);
        Objects.requireNonNull(sessionExpiresAt);

        Duration ttl = Duration.between(clock.instant(), sessionExpiresAt);

        if (ttl.isZero() || ttl.isNegative()) {
            throw new AuthException(AuthErrorCode.SESSION_ALREADY_EXPIRED);
        }

        String key = sessionKey(sessionId);
        String value = registryValue(accountId, tokenId);

        try {
            Boolean registered = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);

            if(!Boolean.TRUE.equals(registered)) {
                throw new IllegalStateException(
                        "Access token session is already registered: "
                                + sessionId
                );
            }
        } catch (DataAccessException e) {
            throw registryUnavailable();
        }
    }

    public boolean isCurrent(UUID accountId, UUID sessionId, UUID tokenId) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(tokenId);

        String expectedValue = registryValue(accountId, tokenId);

        try {
            String currentValue = stringRedisTemplate.opsForValue().get(sessionKey(sessionId));

            return expectedValue.equals(currentValue);
        } catch (DataAccessException exception) {
            throw registryUnavailable();
        }
    }

    public boolean rotate(
            UUID accountId,
            UUID sessionId,
            UUID newTokenId,
            Instant sessionExpiresAt
    ) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(newTokenId);
        Objects.requireNonNull(sessionExpiresAt);

        Duration ttl = Duration.between(
                clock.instant(),
                sessionExpiresAt
        );

        long ttlMillis = ttl.toMillis();

        if (ttlMillis <= 0) {
            return false;
        }

        String accountPrefix = accountId + ":";
        String newValue = registryValue(
                accountId,
                newTokenId
        );

        try {
            Long result = stringRedisTemplate.execute(
                    ROTATE_ACCESS_TOKEN_SCRIPT,
                    List.of(sessionKey(sessionId)),
                    accountPrefix,
                    newValue,
                    Long.toString(ttlMillis)
            );

            return Long.valueOf(1L).equals(result);
        } catch (DataAccessException exception) {
            throw registryUnavailable();
        }
    }

    public boolean revoke(
            UUID accountId,
            UUID sessionId
    ) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);

        String accountPrefix = accountId + ":";

        try {
            Long result = stringRedisTemplate.execute(
                    REVOKE_ACCESS_TOKEN_SCRIPT,
                    List.of(sessionKey(sessionId)),
                    accountPrefix
            );

            return Long.valueOf(1L).equals(result);
        } catch (DataAccessException exception) {
            throw registryUnavailable();
        }
    }

    // --- helper ---
    private static final DefaultRedisScript<Long>
            ROTATE_ACCESS_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                local current = redis.call('GET', KEYS[1])

                if not current then
                    return 0
                end

                if string.sub(
                    current,
                    1,
                    string.len(ARGV[1])
                ) ~= ARGV[1] then
                    return -1
                end

                redis.call(
                    'SET',
                    KEYS[1],
                    ARGV[2],
                    'PX',
                    ARGV[3]
                )

                return 1
                """, Long.class);

    private static final DefaultRedisScript<Long>
            REVOKE_ACCESS_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])

            if not current then
                return 0
            end

            if string.sub(
                current,
                1,
                string.len(ARGV[1])
            ) ~= ARGV[1] then
                return -1
            end

            return redis.call('DEL', KEYS[1])
            """, Long.class);

    private String sessionKey(UUID sessionId) {
        return SESSION_KEY_PREFIX + sessionId.toString();
    }

    private String registryValue(UUID accountId, UUID tokenId) {
        return accountId.toString() + ":" + tokenId.toString();
    }

    private AuthException registryUnavailable() {
        return new AuthException(AuthErrorCode.SESSION_REGISTRY_UNAVAILABLE);
    }
}
