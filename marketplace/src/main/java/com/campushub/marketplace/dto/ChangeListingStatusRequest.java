package com.campushub.marketplace.dto;

import com.campushub.marketplace.domain.ListingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeListingStatusRequest(

        @NotNull(message = "Listing status is required")
        ListingStatus status,

        @NotNull(message = "Expected version is required")
        @PositiveOrZero(
                message = "Expected version cannot be negative"
        )
        Long expectedVersion
) {
}