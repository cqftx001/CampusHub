package com.campushub.marketplace.dto;

import com.campushub.marketplace.domain.ListingCondition;

import java.math.BigDecimal;

/**
 * Paging criteria
 * @param categorySlug
 * @param brandSlug
 * @param condition
 * @param minimumPrice
 * @param maximumPrice
 */
public record ListingSearchCriteria(
        String keyword,
        String categorySlug,
        String brandSlug,
        ListingCondition condition,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice
) {

    public static final int MAXIMUM_KEYWORD_LENGTH = 100;

}
