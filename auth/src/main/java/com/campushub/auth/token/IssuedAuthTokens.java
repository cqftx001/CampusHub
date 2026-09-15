package com.campushub.auth.token;

import java.time.Instant;
import java.util.Objects;

public record IssuedAuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Instant sessionExpiresAt
){

    public IssuedAuthTokens {
        Objects.requireNonNull(accessToken);
        Objects.requireNonNull(refreshToken);
        Objects.requireNonNull(tokenType);
        Objects.requireNonNull(sessionExpiresAt);

        if(accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must no be blank");
        }

        if(refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be blank");
        }

        if(expiresInSeconds <= 0) {
            throw new IllegalArgumentException("expiresInSeconds must be positive");
        }
    }
}
