package com.campushub.auth.vo;

import java.time.Instant;
import java.util.Objects;

public record LoginView(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Instant refreshTokenExpiresAt
) {
    public LoginView {
        Objects.requireNonNull(accessToken);
        Objects.requireNonNull(refreshToken);
        Objects.requireNonNull(tokenType);
        Objects.requireNonNull(refreshTokenExpiresAt);

        if(expiresInSeconds <= 0) {
            throw new IllegalArgumentException("expiresInSeconds must be greater than zero");
        }
    }
}
