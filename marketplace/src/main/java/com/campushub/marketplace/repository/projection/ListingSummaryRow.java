package com.campushub.marketplace.repository.projection;

import com.campushub.marketplace.domain.DeliveryMethod;
import com.campushub.marketplace.domain.ListingCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListingSummaryRow(
        UUID id,
        String categorySlug,
        String categoryDisplayName,
        String parentCategorySlug,
        String parentCategoryDisplayName,
        String brandSlug,
        String brandDisplayName,
        String title,
        ListingCondition condition,
        BigDecimal price,
        String currency,
        String location,
        DeliveryMethod deliveryMethod,
        String primaryImageUrl,
        Instant createdAt
) {
}