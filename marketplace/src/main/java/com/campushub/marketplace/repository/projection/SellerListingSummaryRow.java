package com.campushub.marketplace.repository.projection;

import com.campushub.marketplace.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SellerListingSummaryRow(
        UUID id,
        String title,
        BigDecimal price,
        String currency,
        String primaryImageUrl,
        ListingStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}