package com.campushub.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "campushub.auth.jwt")
public record JwtProperties(
        @NotBlank
        String issuer,

        @NotBlank
        String audience,

        @NotBlank
        String secret,

        @NotNull
        Duration accessTokenTtl
) {

    public JwtProperties {
        if (accessTokenTtl != null
                && (accessTokenTtl.isZero()
                || accessTokenTtl.isNegative())) {
            throw new IllegalArgumentException(
                    "AccessTokenTtl must be positive"
            );
        }
    }
}