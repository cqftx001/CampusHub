package com.campushub.marketplace.vo;

import com.campushub.marketplace.domain.DeliveryMethod;
import com.campushub.marketplace.domain.ListingCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListingSummaryView(
        UUID id,
        CategoryReferenceView category,
        BrandReferenceView brand,
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
