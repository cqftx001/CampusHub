package com.campushub.marketplace.controller;

import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.dto.CreateListingRequest;
import com.campushub.marketplace.dto.ListingSearchCriteria;
import com.campushub.marketplace.service.ListingService;
import com.campushub.marketplace.vo.ListingPageView;
import com.campushub.marketplace.vo.ListingSummaryView;
import com.campushub.marketplace.vo.ListingView;
import com.campushub.marketplace.vo.SellerListingPageView;
import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.security.AuthenticatedAccount;
import com.campushub.shared.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/marketplace/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    public ResponseEntity<ResponseResult<ListingView>> createListing(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody CreateListingRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(httpServletRequest);

        ListingView listing = listingService.createListing(account.accountId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseResult.success(listing, requestId));
    }

    @GetMapping
    public ResponseEntity<ResponseResult<ListingPageView>> getListings(
            @RequestParam(
                    name = "q",
                    required = false
            )
            @Size(
                    max = ListingSearchCriteria.MAXIMUM_KEYWORD_LENGTH,
                    message = "Search keyword cannot exceed 100 characters"
            )
            String keyword,

            @RequestParam(required = false)
            String category,

            @RequestParam(required = false)
            String brand,

            @RequestParam(required = false)
            ListingCondition condition,

            @RequestParam(required = false)
            @DecimalMin(
                    value = "0.00",
                    message = "Minimum price cannot be negative"
            )
            @Digits(
                    integer = 10,
                    fraction = 2,
                    message = "Minimum price is invalid"
            )
            BigDecimal minPrice,

            @RequestParam(required = false)
            @DecimalMin(
                    value = "0.00",
                    message = "Maximum price cannot be negative"
            )
            @Digits(
                    integer = 10,
                    fraction = 2,
                    message = "Maximum price is invalid"
            )
            BigDecimal maxPrice,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Page must not be negative"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Page size must be positive"
            )
            @Max(
                    value = 50,
                    message = "Page size cannot exceed 50"
            )
            int size,

            HttpServletRequest servletRequest
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        ListingSearchCriteria searchCriteria = new ListingSearchCriteria(
                keyword,
                category,
                brand,
                condition,
                minPrice,
                maxPrice
        );

        ListingPageView listings = listingService.searchActiveListings(
                searchCriteria ,
                page,
                size
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(listings, requestId));
    }

    @GetMapping("/mine")
    public ResponseEntity<ResponseResult<SellerListingPageView>> getMyListings(
            @AuthenticationPrincipal
            AuthenticatedAccount account,

            @RequestParam(required = false)
            ListingStatus status,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must not be negative")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be positive")
            @Max(value = 50, message = "Page size cannot exceed 50")
            int size,

            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);

        SellerListingPageView listings =
                listingService.searchSellerListing(
                        account.accountId(),
                        status,
                        page,
                        size
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(listings, requestId));
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<ResponseResult<ListingView>> getListing(
        @PathVariable UUID listingId,
        HttpServletRequest httpServletRequest
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(httpServletRequest);

        ListingView listing =  listingService.getActiveListing(listingId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(listing, requestId));
    }

}
