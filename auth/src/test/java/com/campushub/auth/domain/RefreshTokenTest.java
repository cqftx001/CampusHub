package com.campushub.auth.domain;

import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class RefreshTokenTest {

    private static final String TOKEN_HASH = "a".repeat(64);

    @Test
    void activeTokenCanBeRotatedOnce() {
        Instant issuedAt = Instant.parse("2026-08-25T10:00:00Z");
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                TOKEN_HASH,
                issuedAt,
                issuedAt.plusSeconds(3600)
        );
        UUID replacementId = UUID.randomUUID();

        token.markUsed(
                issuedAt.plusSeconds(60),
                replacementId
        );

        assertThat(token.getStatus())
                .isEqualTo(RefreshTokenStatus.USED);
        assertThat(token.getReplacedByTokenId())
                .isEqualTo(replacementId);
    }

    @Test
    void usedTokenCannotBeRotatedAgain() {
        Instant issuedAt = Instant.parse("2026-08-25T10:00:00Z");
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                TOKEN_HASH,
                issuedAt,
                issuedAt.plusSeconds(3600)
        );

        token.markUsed(
                issuedAt.plusSeconds(30),
                UUID.randomUUID()
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> token.markUsed(
                        issuedAt.plusSeconds(20),
                        UUID.randomUUID()
                )
        );

        assertEquals(
                AuthErrorCode.REFRESH_TOKEN_NON_USABLE,
                exception.getErrorCode()
        );
    }

    @Test
    void activeTokenCanBeRevokedIdempotently() {
        Instant issuedAt = Instant.parse("2026-08-25T10:00:00Z");
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                TOKEN_HASH,
                issuedAt,
                issuedAt.plusSeconds(3600)
        );

        Instant firstRevocation = issuedAt.plusSeconds(30);
        token.revoke(firstRevocation);
        token.revoke(issuedAt.plusSeconds(60));

        assertThat(token.getStatus())
                .isEqualTo(RefreshTokenStatus.REVOKED);
        assertThat(token.getRevokedAt())
                .isEqualTo(firstRevocation);
    }

}
