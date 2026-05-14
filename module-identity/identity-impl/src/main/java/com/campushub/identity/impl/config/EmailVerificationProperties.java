package com.campushub.identity.impl.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "identity.email-verification")
public record EmailVerificationProperties(

        @NotNull
        Duration codeTtl,

        @NotNull
        Duration resendInterval,

        @Min(1)
        int maxAttempts,

        @NotBlank
        String codePepper
) {
    public EmailVerificationProperties {
        requirePositive(codeTtl, "codeTtl");
        requirePositive(resendInterval, "resendInterval");
    }

    private static void requirePositive(Duration duration, String fieldName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
