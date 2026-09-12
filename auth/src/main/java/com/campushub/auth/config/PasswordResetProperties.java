package com.campushub.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "campushub.auth.password-reset")
public record PasswordResetProperties(
        @NotNull
        Duration tokenTtl,

        @NotNull
        Duration requestCooldown,

        @NotBlank
        String resetUrl,

        @NotBlank
        String fromAddress
) {

    public PasswordResetProperties {
        requirePositive(tokenTtl, "tokenTtl");
        requirePositive(requestCooldown, "requestCooldown");
    }

    private static void requirePositive(Duration duration, String propertyName) {
        if(duration != null && (duration.isZero() || duration.isNegative() || duration.toMillis() == 0)) {
            throw new IllegalArgumentException(propertyName + " must be at least one millisecond");
        }

    }
}
