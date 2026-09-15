package com.campushub.auth.security;

import com.campushub.auth.config.RefreshCookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class RefreshTokenCookieManager {

    public static final String COOKIE_NAME =
            "campushub_refresh_token";

    public static final String COOKIE_PATH =
            "/api/auth";

    private final RefreshCookieProperties properties;
    private final Clock clock;

    public RefreshTokenCookieManager(
            RefreshCookieProperties properties,
            Clock clock
    ) {
        this.properties = properties;
        this.clock = clock;
    }

    public ResponseCookie create(
            String rawRefreshToken,
            Instant expiresAt
    ) {
        Objects.requireNonNull(
                rawRefreshToken,
                "rawRefreshToken must not be null"
        );

        Objects.requireNonNull(
                expiresAt,
                "expiresAt must not be null"
        );

        Duration maxAge = Duration.between(
                clock.instant(),
                expiresAt
        );

        if (maxAge.isNegative()) {
            maxAge = Duration.ZERO;
        }

        return baseCookie(rawRefreshToken)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(
            String value
    ) {
        return ResponseCookie
                .from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(COOKIE_PATH);
    }
}