package com.campushub.shared.utils;

import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {}

    public static String stripToNull(String value) {
            if (value == null) {
               return null;
            }

            String normalized = value.strip();
            return normalized.isEmpty() ? null : normalized;
    }

    public static String stripAndLowercaseToNull(String value) {
        String normalized = stripToNull(value);

        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
