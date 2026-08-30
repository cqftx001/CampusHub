package com.campushub.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(
        prefix = "campushub.auth.login-rate-limit"
)
public record LoginRateLimitProperties(

        @NotNull
        Duration window,

        @Min(1)
        int identifierMaxFailures,

        @Min(1)
        int ipMaxFailures
) {

    public LoginRateLimitProperties {
        if (window != null
                && (window.isZero()
                || window.isNegative()
                || window.toMillis() == 0)) {
            throw new IllegalArgumentException(
                    "Login rate-limit window must be at least one millisecond"
            );
        }

        if (identifierMaxFailures > 0
                && ipMaxFailures > 0
                && ipMaxFailures < identifierMaxFailures) {
            throw new IllegalArgumentException(
                    "IP failure limit must not be lower than identifier failure limit"
            );
        }
    }
}