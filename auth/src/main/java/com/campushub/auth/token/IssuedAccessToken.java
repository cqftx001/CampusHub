package com.campushub.auth.token;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JWT签发结果
 * @param value
 * @param expiresAt
 */
public record IssuedAccessToken(
        String value,
        UUID tokenId,
        Instant expiresAt
) {

    public IssuedAccessToken {
        Objects.requireNonNull(tokenId);
        Objects.requireNonNull(value);
        Objects.requireNonNull(expiresAt);
    }
}