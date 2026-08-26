package com.campushub.auth.domain;

import com.campushub.auth.error.AuthException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LoginSessionTest {

    @Test
    void activeSessionCanBeMarkedAsUsed() {
        Instant startedAt = Instant.parse("2026-08-25T10:00:00Z");
        LoginSession session = new LoginSession(
                UUID.randomUUID(),
                startedAt,
                startedAt.plusSeconds(3600),
                "JUnit",
                "127.0.0.1"
        );

        Instant usedAt = startedAt.plusSeconds(60);
        session.markUsed(usedAt);

        assertThat(session.isActive(usedAt)).isTrue();
        assertThat(session.getLastUsedAt()).isEqualTo(usedAt);
    }

    @Test
    void revokeIsIdempotent() {
        Instant startedAt = Instant.parse("2026-08-25T10:00:00Z");
        LoginSession session = new LoginSession(
                UUID.randomUUID(),
                startedAt,
                startedAt.plusSeconds(3600),
                null,
                null
        );

        Instant firstRevocation = startedAt.plusSeconds(30);
        session.revoke(firstRevocation);
        session.revoke(startedAt.plusSeconds(60));

        assertThat(session.getStatus())
                .isEqualTo(LoginSessionStatus.REVOKED);
        assertThat(session.getRevokedAt())
                .isEqualTo(firstRevocation);
        assertThat(session.isActive(firstRevocation)).isFalse();
    }

    @Test
    void expiredSessionCannotBeMarkedAsUsed() {
        Instant startedAt = Instant.parse("2026-08-25T10:00:00Z");
        Instant expiresAt = startedAt.plusSeconds(60);
        LoginSession session = new LoginSession(
                UUID.randomUUID(),
                startedAt,
                expiresAt,
                null,
                null
        );

        assertThatThrownBy(() -> session.markUsed(expiresAt))
                .isInstanceOf(AuthException.class);
    }
}
