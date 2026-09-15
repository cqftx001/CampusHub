package com.campushub.auth.config;

import com.campushub.shared.utils.TextNormalizer;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "campushub.auth.refresh-cookie")
public record RefreshCookieProperties(
        @DefaultValue("false")
        boolean secure,

        @DefaultValue("Strict")
        @NotBlank
        String sameSite
) {

    private static final Set<String> ALLOWED_SAME_SITE_VALUES =
            Set.of("Strict", "Lax", "None");

    public RefreshCookieProperties {
        sameSite = normalizeSameSite(sameSite);

        if (!ALLOWED_SAME_SITE_VALUES.contains(sameSite)) {
            throw new IllegalArgumentException(
                    "Refresh cookie SameSite must be Strict, Lax, or None"
            );
        }

        if ("None".equals(sameSite) && !secure) {
            throw new IllegalArgumentException(
                    "SameSite=None requires a secure cookie"
            );
        }
    }

    private static String normalizeSameSite(String value) {
        String normalized =
                TextNormalizer.stripAndLowercaseToNull(value);

        if (normalized == null) {
            return null;
        }

        return switch (normalized) {
            case "strict" -> "Strict";
            case "lax" -> "Lax";
            case "none" -> "None";
            default -> value;
        };
    }
}