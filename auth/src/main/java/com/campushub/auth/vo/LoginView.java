package com.campushub.auth.vo;

import com.campushub.auth.token.IssuedAuthTokens;

import java.time.Instant;
import java.util.Objects;

public record LoginView(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Instant sessionExpiresAt
) {

    public LoginView {
        Objects.requireNonNull(accessToken);
        Objects.requireNonNull(tokenType);
        Objects.requireNonNull(sessionExpiresAt);

        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException(
                    "expiresInSeconds must be greater than zero"
            );
        }
    }

    public static LoginView from(IssuedAuthTokens tokens) {
        Objects.requireNonNull(tokens);

        return new LoginView(
                tokens.accessToken(),
                tokens.tokenType(),
                tokens.expiresInSeconds(),
                tokens.sessionExpiresAt()
        );
    }
}