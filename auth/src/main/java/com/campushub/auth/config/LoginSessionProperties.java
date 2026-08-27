package com.campushub.auth.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "campushub.auth.login-session")
public record LoginSessionProperties(
    @NotNull
    Duration ttl
) {
    public LoginSessionProperties {
        if(ttl != null && (ttl.isZero() || ttl.isNegative())) {
            throw new IllegalArgumentException("Login session ttl must be positive");
        }
    }
}
