package com.campushub.auth.utils;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AuthInputNormalizer {

    public String normalizeCaseInsensitive(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value
                .strip()
                .toLowerCase(Locale.ROOT);

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}