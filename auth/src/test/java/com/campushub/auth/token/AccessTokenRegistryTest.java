package com.campushub.auth.token;

import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenRegistryTest {

    private static final Instant NOW =
            Instant.parse("2026-08-28T12:00:00Z");

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AccessTokenRegistry registry;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        registry = new AccessTokenRegistry(
                stringRedisTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registersCurrentAccessTokenForSession() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        when(valueOperations.setIfAbsent(
                "auth:access:session:" + sessionId,
                accountId + ":" + tokenId,
                Duration.ofDays(30)
        )).thenReturn(true);

        registry.register(
                accountId,
                sessionId,
                tokenId,
                NOW.plus(Duration.ofDays(30))
        );

        verify(valueOperations).setIfAbsent(
                "auth:access:session:" + sessionId,
                accountId + ":" + tokenId,
                Duration.ofDays(30)
        );
    }

    @Test
    void redisFailureRejectsRegistration() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        when(valueOperations.setIfAbsent(
                "auth:access:session:" + sessionId,
                accountId + ":" + tokenId,
                Duration.ofDays(30)
        )).thenThrow(
                new RedisConnectionFailureException(
                        "Redis unavailable"
                )
        );

        assertThatThrownBy(() -> registry.register(
                accountId,
                sessionId,
                tokenId,
                NOW.plus(Duration.ofDays(30))
        ))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> assertThat(
                        ((AuthException) exception).getErrorCode()
                ).isEqualTo(
                        AuthErrorCode.SESSION_REGISTRY_UNAVAILABLE
                ));
    }

    @Test
    void matchingTokenIsCurrent() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        when(valueOperations.get(
                "auth:access:session:" + sessionId
        )).thenReturn(
                accountId + ":" + tokenId
        );

        assertThat(registry.isCurrent(
                accountId,
                sessionId,
                tokenId
        )).isTrue();
    }

    @Test
    void missingSessionIsNotCurrent() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        when(valueOperations.get(
                "auth:access:session:" + sessionId
        )).thenReturn(null);

        assertThat(registry.isCurrent(
                accountId,
                sessionId,
                tokenId
        )).isFalse();
    }

    @Test
    void differentTokenIsNotCurrent() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(valueOperations.get(
                "auth:access:session:" + sessionId
        )).thenReturn(
                accountId + ":" + UUID.randomUUID()
        );

        assertThat(registry.isCurrent(
                accountId,
                sessionId,
                UUID.randomUUID()
        )).isFalse();
    }

    @Test
    void redisFailureRejectsValidation() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        when(valueOperations.get(
                "auth:access:session:" + sessionId
        )).thenThrow(
                new RedisConnectionFailureException(
                        "Redis unavailable"
                )
        );

        assertThatThrownBy(() -> registry.isCurrent(
                accountId,
                sessionId,
                tokenId
        ))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> assertThat(
                        ((AuthException) exception).getErrorCode()
                ).isEqualTo(
                        AuthErrorCode.SESSION_REGISTRY_UNAVAILABLE
                ));
    }

    @Test
    void revokesCurrentAccessTokenForSession() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers
                        .<RedisScript<Long>>any(),
                eq(List.of(
                        "auth:access:session:" + sessionId
                )),
                eq(accountId + ":")
        )).thenReturn(1L);

        boolean revoked = registry.revoke(
                accountId,
                sessionId
        );

        assertThat(revoked).isTrue();
    }

    @Test
    void missingOnlineSessionIsAnIdempotentNoOp() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers
                        .<RedisScript<Long>>any(),
                eq(List.of(
                        "auth:access:session:" + sessionId
                )),
                eq(accountId + ":")
        )).thenReturn(0L);

        boolean revoked = registry.revoke(
                accountId,
                sessionId
        );

        assertThat(revoked).isFalse();
    }

    @Test
    void anotherAccountsOnlineSessionIsNotDeleted() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers
                        .<RedisScript<Long>>any(),
                eq(List.of(
                        "auth:access:session:" + sessionId
                )),
                eq(accountId + ":")
        )).thenReturn(-1L);

        boolean revoked = registry.revoke(
                accountId,
                sessionId
        );

        assertThat(revoked).isFalse();
    }

    @Test
    void redisFailureRejectsRevocation() {
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers
                        .<RedisScript<Long>>any(),
                eq(List.of(
                        "auth:access:session:" + sessionId
                )),
                eq(accountId + ":")
        )).thenThrow(
                new RedisConnectionFailureException(
                        "Redis unavailable"
                )
        );

        assertThatThrownBy(() -> registry.revoke(
                accountId,
                sessionId
        ))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> assertThat(
                        ((AuthException) exception).getErrorCode()
                ).isEqualTo(
                        AuthErrorCode.SESSION_REGISTRY_UNAVAILABLE
                ));
    }
}