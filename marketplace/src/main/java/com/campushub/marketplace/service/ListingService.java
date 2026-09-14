package com.campushub.marketplace.service;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.dto.CreateListingRequest;
import com.campushub.marketplace.dto.ListingSearchCriteria;
import com.campushub.marketplace.vo.ListingPageView;
import com.campushub.marketplace.vo.ListingView;
import com.campushub.marketplace.vo.SellerListingPageView;

import java.util.UUID;

public interface ListingService {

    ListingView createListing(
            UUID sellerAccountId,
            CreateListingRequest request
    );

    ListingView getActiveListing(UUID listingId);

    ListingPageView searchActiveListings(
            ListingSearchCriteria criteria,
            int page,
            int size
    );

    SellerListingPageView searchSellerListing(
            UUID sellerAccountId,
            ListingStatus status,
            int page,
            int size
    );
}
