package com.campushub.auth.security;

import com.campushub.auth.config.LoginRateLimitProperties;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class LoginAttemptLimiter {

    private static final String IDENTIFIER_KEY_PREFIX =
            "auth:login:failure:identifier:";

    private static final String IP_KEY_PREFIX =
            "auth:login:failure:ip:";

    /*
     * 返回值：
     * 0  -> 当前允许继续验证密码
     * >0 -> 剩余限制时间，单位毫秒
     */
    private static final DefaultRedisScript<Long> CHECK_LIMIT_SCRIPT =
            new DefaultRedisScript<>("""
                    local blockedTtl = 0

                    for index, key in ipairs(KEYS) do
                        local count = tonumber(
                            redis.call('GET', key) or '0'
                        )
                        local maximum = tonumber(ARGV[index])

                        if count >= maximum then
                            local ttl = redis.call('PTTL', key)

                            if ttl < 1 then
                                ttl = 1
                            end

                            if ttl > blockedTtl then
                                blockedTtl = ttl
                            end
                        end
                    end

                    return blockedTtl
                    """, Long.class);

    /*
     * 同时增加 identifier 和 IP 失败次数，并只在第一次失败时设置 TTL。
     *
     * count > maximum 时，本次请求直接进入限流状态。
     * 因此 maximum = 5 时，前五次返回普通 credential error，
     * 第六次开始返回 rate limited。
     */
    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT =
            new DefaultRedisScript<>("""
                    local windowMillis = ARGV[1]
                    local blockedTtl = 0

                    for index, key in ipairs(KEYS) do
                        local count = redis.call('INCR', key)

                        if count == 1 then
                            redis.call(
                                'PEXPIRE',
                                key,
                                windowMillis
                            )
                        elseif redis.call('PTTL', key) < 0 then
                            redis.call(
                                'PEXPIRE',
                                key,
                                windowMillis
                            )
                        end

                        local maximum =
                            tonumber(ARGV[index + 1])

                        if count > maximum then
                            local ttl = redis.call('PTTL', key)

                            if ttl < 1 then
                                ttl = 1
                            end

                            if ttl > blockedTtl then
                                blockedTtl = ttl
                            end
                        end
                    end

                    return blockedTtl
                    """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginRateLimitProperties properties;

    public LoginAttemptLimiter(
            StringRedisTemplate stringRedisTemplate,
            LoginRateLimitProperties properties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    public void assertAllowed(
            String identifier,
            String ipAddress
    ) {
        LimitKeys limitKeys = createLimitKeys(
                identifier,
                ipAddress
        );

        Object[] arguments = limitKeys.limits()
                .stream()
                .map(String::valueOf)
                .toArray();

        try {
            Long blockedTtl = stringRedisTemplate.execute(
                    CHECK_LIMIT_SCRIPT,
                    limitKeys.keys(),
                    arguments
            );

            if (blockedTtl == null) {
                throw protectionUnavailable();
            }

            if (blockedTtl > 0) {
                throw rateLimited();
            }
        } catch (DataAccessException exception) {
            throw protectionUnavailable();
        }
    }

    public void recordFailure(
            String identifier,
            String ipAddress
    ) {
        LimitKeys limitKeys = createLimitKeys(
                identifier,
                ipAddress
        );

        Object[] arguments = new Object[limitKeys.keys().size() + 1];

        arguments[0] = Long.toString(properties.window().toMillis());

        for (int index = 0;
             index < limitKeys.limits().size();
             index++) {
            arguments[index + 1] =
                    Integer.toString(
                            limitKeys.limits().get(index)
                    );
        }

        try {
            Long blockedTtl = stringRedisTemplate.execute(
                    RECORD_FAILURE_SCRIPT,
                    limitKeys.keys(),
                    arguments
            );

            if (blockedTtl == null) {
                throw protectionUnavailable();
            }

            if (blockedTtl > 0) {
                throw rateLimited();
            }
        } catch (DataAccessException exception) {
            throw protectionUnavailable();
        }
    }

    public void clearIdentifierFailures(String identifier) {
        String normalizedIdentifier =
                normalizeIdentifier(identifier);

        try {
            stringRedisTemplate.delete(
                    identifierKey(normalizedIdentifier)
            );
        } catch (DataAccessException exception) {
            throw protectionUnavailable();
        }
    }

    private LimitKeys createLimitKeys(
            String identifier,
            String ipAddress
    ) {
        String normalizedIdentifier =
                normalizeIdentifier(identifier);

        List<String> keys = new ArrayList<>();
        List<Integer> limits = new ArrayList<>();

        keys.add(identifierKey(normalizedIdentifier));
        limits.add(properties.identifierMaxFailures());

        String normalizedIpAddress = normalizeIpAddress(
                ipAddress
        );

        if (normalizedIpAddress != null) {
            keys.add(
                    IP_KEY_PREFIX + sha256(normalizedIpAddress)
            );
            limits.add(properties.ipMaxFailures());
        }

        return new LimitKeys(
                List.copyOf(keys),
                List.copyOf(limits)
        );
    }

    private String identifierKey(
            String normalizedIdentifier
    ) {
        return IDENTIFIER_KEY_PREFIX
                + sha256(normalizedIdentifier);
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return "<missing>";
        }

        String normalized = identifier
                .strip()
                .toLowerCase(Locale.ROOT);

        return normalized.isEmpty()
                ? "<missing>"
                : normalized;
    }

    private String normalizeIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }

        String normalized = ipAddress.strip();

        return normalized.isEmpty()
                ? null
                : normalized.toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    private AuthException rateLimited() {
        return new AuthException(
                AuthErrorCode.LOGIN_RATE_LIMITED
        );
    }

    private AuthException protectionUnavailable() {
        return new AuthException(
                AuthErrorCode.LOGIN_PROTECTION_UNAVAILABLE
        );
    }

    private record LimitKeys(
            List<String> keys,
            List<Integer> limits
    ) {
    }
}