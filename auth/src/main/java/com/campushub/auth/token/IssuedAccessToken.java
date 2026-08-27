package com.campushub.auth.token;

import java.time.Instant;
import java.util.Objects;

/**
 * JWT签发结果
 * @param value
 * @param expiresAt
 */
public record IssuedAccessToken(
        String value,
        Instant expiresAt
) {

    public IssuedAccessToken {
        Objects.requireNonNull(value);
        Objects.requireNonNull(expiresAt);
    }
}