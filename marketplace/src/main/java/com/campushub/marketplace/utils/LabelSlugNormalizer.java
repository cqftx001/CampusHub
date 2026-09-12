package com.campushub.marketplace.utils;

import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.shared.utils.TextNormalizer;

import java.util.regex.Pattern;

public final class LabelSlugNormalizer {

    public static final int MAXIMUM_LENGTH = 64;

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private LabelSlugNormalizer() {
    }

    public static String normalize(String value) {
        String normalized = TextNormalizer.stripAndLowercaseToNull(value);

        if(normalized == null
                || normalized.length() > MAXIMUM_LENGTH
                || !SLUG_PATTERN.matcher(normalized).matches()) {
            throw new MarketplaceException(MarketplaceErrorCode.INVALID_LABEL_SELECTION);
        }
        return normalized;
    }
}
