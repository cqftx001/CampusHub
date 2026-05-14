package com.campushub.identity.impl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.email-verification")
public record EmailVerificationProperties(
        int codeLength,
        Duration codeTtl,
        Duration resendInterval,
        int maxAttempts,
        String codePepper
) {
}
