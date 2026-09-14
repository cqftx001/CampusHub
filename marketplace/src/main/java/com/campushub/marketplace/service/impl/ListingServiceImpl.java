package com.campushub.marketplace.service.impl;

import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.dto.CreateListingRequest;
import com.campushub.marketplace.dto.ListingSearchCriteria;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.mapper.ListingMapper;
import com.campushub.marketplace.repository.ListingRepository;
import com.campushub.marketplace.repository.projection.ListingSummaryRow;
import com.campushub.marketplace.repository.projection.SellerListingSummaryRow;
import com.campushub.marketplace.service.CatalogSelectionResolver;
import com.campushub.marketplace.service.ListingService;
import com.campushub.marketplace.service.ResolvedCatalogSelection;
import com.campushub.marketplace.utils.CatalogNormalizer;
import com.campushub.marketplace.vo.*;
import com.campushub.shared.utils.TextNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final CatalogSelectionResolver catalogSelectionResolver;
    private final ListingMapper listingMapper;

    public ListingServiceImpl(ListingRepository listingRepository, CatalogSelectionResolver catalogSelectionResolver, ListingMapper listingMapper) {
        this.listingRepository = listingRepository;
        this.catalogSelectionResolver = catalogSelectionResolver;
        this.listingMapper = listingMapper;
    }

    @Override
    @Transactional
    public ListingView createListing(UUID sellerAccountId, CreateListingRequest request) {
        Objects.requireNonNull(sellerAccountId, "Seller account ID cannot be null");
        Objects.requireNonNull(request, "Create listing request cannot be null");

        validateUniqueImageUrls(request);

        ResolvedCatalogSelection selection = catalogSelectionResolver
                .resolveForCreate(
                        request.categorySlug(),
                        request.brandSlug()
                );

        Listing listing = new Listing(
                sellerAccountId,
                selection.category(),
                selection.brand(),
                request.title(),
                request.description(),
                request.manufactureYear(),
                request.condition(),
                request.price(),
                request.location(),
                request.deliveryMethod(),
                request.imageUrls()
        );

        Listing savedListing = listingRepository.saveAndFlush(listing);

        return listingMapper.toView(savedListing);
    }

    @Override
    public ListingPageView searchActiveListings(
            ListingSearchCriteria criteria,
            int page,
            int size
    ) {
        Objects.requireNonNull(criteria, "Listing search criteria cannot be null");
        validatePageRequest(page, size);
        validatePriceRange(criteria.minimumPrice(), criteria.maximumPrice());

        String categorySlug = normalizeOptionalSlug(criteria.categorySlug());
        String brandSlug = normalizeOptionalSlug(criteria.brandSlug());
        String keywordPattern = createKeywordPattern(criteria.keyword());

        PageRequest pageRequest = PageRequest.of(page, size);

        Page<ListingSummaryRow> result = listingRepository
                .searchActiveListingSummaries(
                        ListingStatus.ACTIVE,
                        keywordPattern,
                        categorySlug,
                        brandSlug,
                        criteria.condition(),
                        criteria.minimumPrice(),
                        criteria.maximumPrice(),
                        pageRequest
                        );

        List<ListingSummaryView> items = result
                .getContent()
                .stream()
                .map(listingMapper::toSummaryView)
                .toList();

        return new ListingPageView(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    @Override
    public SellerListingPageView searchSellerListing(
            UUID sellerAccountId,
            ListingStatus status,
            int page,
            int size
    ) {
        Objects.requireNonNull(sellerAccountId, "Seller account ID cannot be null");

        validatePageRequest(page, size);

        PageRequest pageRequest = PageRequest.of(page, size);

        Page<SellerListingSummaryRow> result = listingRepository
                .findSellerListingSummaries(sellerAccountId, status, pageRequest);

        List<SellerListingSummaryView> items =
                result.getContent()
                        .stream()
                        .map(listingMapper::toSellerSummaryView)
                        .toList();

        return new SellerListingPageView(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    @Override
    public ListingView getActiveListing(UUID listingId) {
        Objects.requireNonNull(listingId, "Listing ID cannot be null");

        Listing listing = listingRepository.findByIdAndStatus(listingId, ListingStatus.ACTIVE)
                .orElseThrow(() -> new MarketplaceException(MarketplaceErrorCode.LISTING_NOT_FOUND));

        return listingMapper.toView(listing);
    }

    // --- helper ---
    private String createKeywordPattern(String keyword) {
        String normalized = TextNormalizer.stripAndLowercaseToNull(keyword);

        if(normalized == null) {
            return null;
        }

        if(normalized.length() > ListingSearchCriteria.MAXIMUM_KEYWORD_LENGTH) {
            throw invalidFilters("Search keyword cannot exceed 100 characters");
        }

        // 转义字符: % 任意长度字符匹配; _ 匹配单个任意字符; \\s+ 空格制定任意匹配
        String escaped = normalized
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replaceAll("\\s+", "%");

        return "%" + escaped + "%";
    }

    private String normalizeOptionalSlug(String value) {
        String stripped = TextNormalizer.stripToNull(value);

        if (stripped == null) {
            return null;
        }

        return CatalogNormalizer.normalizeSlug(stripped);
    }

    private void validatePriceRange(
            BigDecimal minimumPrice,
            BigDecimal maximumPrice
    ) {
        if (minimumPrice != null
                && minimumPrice.signum() < 0) {
            throw invalidFilters("Minimum price cannot be negative");
        }

        if (maximumPrice != null
                && maximumPrice.signum() < 0) {
            throw invalidFilters("Maximum price cannot be negative");
        }

        if (minimumPrice != null
                && maximumPrice != null
                && minimumPrice.compareTo(maximumPrice) > 0) {
            throw invalidFilters("Minimum price cannot exceed maximum price");
        }
    }

    private MarketplaceException invalidFilters(String message) {
        return new MarketplaceException(MarketplaceErrorCode.INVALID_LISTING_FILTERS, message);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw invalidFilters("Page must not be negative");
        }

        if (size < 1 || size > 50) {
            throw invalidFilters("Page size must be between 1 and 50");
        }
    }

    private void validateUniqueImageUrls(CreateListingRequest request) {
        if(new HashSet<>(request.imageUrls()).size() != request.imageUrls().size()) {
            throw new MarketplaceException(MarketplaceErrorCode.INVALID_LISTING_DETAILS, "Image URLs cannot be duplicated");
        }
    }
}
