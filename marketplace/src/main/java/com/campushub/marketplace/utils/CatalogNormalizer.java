package com.campushub.marketplace.utils;

import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.shared.utils.TextNormalizer;

import java.util.regex.Pattern;

public final class CatalogNormalizer {

    public static final int MAXIMUM_SLUG_LENGTH = 64;
    public static final int MAXIMUM_DISPLAY_NAME_LENGTH = 100;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private CatalogNormalizer() {}

    /**
     * normalizeSlug
     * @param value
     * @return
     */
    public static String normalizeSlug(String value) {
        String normalized = TextNormalizer.stripAndLowercaseToNull(value);

        if(normalized == null || normalized.length() > MAXIMUM_SLUG_LENGTH || !SLUG_PATTERN.matcher(normalized).matches()) {
            throw new MarketplaceException(MarketplaceErrorCode.INVALID_CATALOG_SLUG);
        }
        return normalized;
    }

    /**
     * normalizeDisplayName
     * @param value
     * @return
     */
    public static String normalizeDisplayName(String value) {
        String normalized = TextNormalizer.stripAndLowercaseToNull(value);

        if(normalized == null) {
            throw new IllegalArgumentException("Display name cannot be null or empty");
        }

        if(normalized.length() > MAXIMUM_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Display name cannot be longer than " + MAXIMUM_DISPLAY_NAME_LENGTH);
        }
        return normalized;
    }

    /**
     * requireDisplayOrder
     * @param value
     * @return
     */
    public static int requireDisplayOrder(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Display order cannot be negative");
        }
        return value;
    }
}
