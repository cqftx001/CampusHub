package com.campushub.marketplace.vo;

import java.util.List;

public record ListingPageView(
        List<ListingSummaryView> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public ListingPageView {
        items = List.copyOf(items);
    }
}
