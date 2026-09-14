package com.campushub.marketplace.vo;

import java.util.List;

public record SellerListingPageView (
        List<SellerListingSummaryView> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
){

    public SellerListingPageView {
        items = List.copyOf(items);
    }
}
