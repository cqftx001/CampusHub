package com.campushub.marketplace.vo;

import com.campushub.marketplace.domain.DeliveryMethod;
import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListingView(
        UUID id,
        UUID sellerAccountId,
        CategoryReferenceView category,
        BrandReferenceView brand,
        String title,
        String description,
        Integer manufactureYear,
        ListingCondition condition,
        BigDecimal price,
        String currency,
        String location,
        DeliveryMethod deliveryMethod,
        List<String> imageUrls,
        ListingStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public ListingView {
        imageUrls = List.copyOf(imageUrls);
    }
}
