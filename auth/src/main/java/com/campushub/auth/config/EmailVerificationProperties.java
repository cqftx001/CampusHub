package com.campushub.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(
        prefix = "campushub.auth.email-verification"
)
public record EmailVerificationProperties(

        @NotNull
        Duration tokenTtl,

        @NotNull
        Duration resendCoolDown,

        @NotBlank
        String verificationUrl,

        @NotBlank
        String fromAddress
) {
}
