package com.campushub.marketplace.service.impl;

import com.campushub.marketplace.domain.*;
import com.campushub.marketplace.dto.ChangeListingStatusRequest;
import com.campushub.marketplace.dto.ListingSearchCriteria;
import com.campushub.marketplace.dto.UpdateListingRequest;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.mapper.ListingMapper;
import com.campushub.marketplace.repository.ListingRepository;
import com.campushub.marketplace.repository.projection.ListingSummaryRow;
import com.campushub.marketplace.service.CatalogSelectionResolver;
import com.campushub.marketplace.service.ResolvedCatalogSelection;
import com.campushub.marketplace.vo.ListingPageView;
import com.campushub.marketplace.vo.ListingView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceImplTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CatalogSelectionResolver catalogSelectionResolver;

    private ListingServiceImpl listingService;

    @BeforeEach
    void setUp() {
        listingService = new ListingServiceImpl(
                listingRepository,
                catalogSelectionResolver,
                new ListingMapper()
        );
    }

    @Test
    void searchNormalizesKeywordAndCatalogSlugs() {
        PageRequest pageRequest =
                PageRequest.of(1, 20);

        when(listingRepository.searchActiveListingSummaries(
                any(ListingStatus.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(ListingCondition.class),
                nullable(BigDecimal.class),
                nullable(BigDecimal.class),
                any(Pageable.class)
        )).thenReturn(Page.empty(pageRequest));

        ListingSearchCriteria criteria =
                new ListingSearchCriteria(
                        "  RTX   5090_100%!  ",
                        "  ELECTRONICS  ",
                        "  NVIDIA  ",
                        ListingCondition.NEW,
                        new BigDecimal("1000.00"),
                        new BigDecimal("2500.00")
                );

        listingService.searchActiveListings(
                criteria,
                1,
                20
        );

        verify(listingRepository)
                .searchActiveListingSummaries(
                        ListingStatus.ACTIVE,
                        "%rtx%5090!_100!%!!%",
                        "electronics",
                        "nvidia",
                        ListingCondition.NEW,
                        new BigDecimal("1000.00"),
                        new BigDecimal("2500.00"),
                        pageRequest
                );
    }

    @Test
    void searchMapsProjectionAndPaginationMetadata() {
        UUID listingId = UUID.randomUUID();
        Instant createdAt =
                Instant.parse("2026-09-14T10:00:00Z");

        ListingSummaryRow row =
                new ListingSummaryRow(
                        listingId,
                        "graphics-cards",
                        "Graphics Cards",
                        "electronics",
                        "Electronics",
                        "nvidia",
                        "NVIDIA",
                        "NVIDIA RTX 5090",
                        ListingCondition.LIKE_NEW,
                        new BigDecimal("1999.99"),
                        "USD",
                        "Los Angeles, CA",
                        DeliveryMethod.LOCAL_PICKUP,
                        "https://images.example.com/rtx-5090.jpg",
                        createdAt
                );

        PageRequest pageRequest =
                PageRequest.of(0, 1);

        Page<ListingSummaryRow> repositoryPage =
                new PageImpl<>(
                        List.of(row),
                        pageRequest,
                        2
                );

        when(listingRepository.searchActiveListingSummaries(
                ListingStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                pageRequest
        )).thenReturn(repositoryPage);

        ListingPageView result =
                listingService.searchActiveListings(
                        new ListingSearchCriteria(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        0,
                        1
                );

        assertThat(result.items())
                .hasSize(1);

        assertThat(result.items().getFirst().id())
                .isEqualTo(listingId);

        assertThat(result.items().getFirst().title())
                .isEqualTo("NVIDIA RTX 5090");

        assertThat(
                result.items()
                        .getFirst()
                        .category()
                        .slug()
        ).isEqualTo("graphics-cards");

        assertThat(
                result.items()
                        .getFirst()
                        .category()
                        .parentSlug()
        ).isEqualTo("electronics");

        assertThat(
                result.items()
                        .getFirst()
                        .brand()
                        .slug()
        ).isEqualTo("nvidia");

        assertThat(result.page())
                .isZero();

        assertThat(result.size())
                .isEqualTo(1);

        assertThat(result.totalElements())
                .isEqualTo(2);

        assertThat(result.totalPages())
                .isEqualTo(2);

        assertThat(result.hasNext())
                .isTrue();

        assertThat(result.hasPrevious())
                .isFalse();
    }

    @Test
    void reversedPriceRangeIsRejectedBeforeRepositoryCall() {
        ListingSearchCriteria criteria =
                new ListingSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("500.00"),
                        new BigDecimal("100.00")
                );

        MarketplaceException exception =
                assertThrows(
                        MarketplaceException.class,
                        () -> listingService.searchActiveListings(
                                criteria,
                                0,
                                20
                        )
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        MarketplaceErrorCode.INVALID_LISTING_FILTERS
                );

        assertThat(exception)
                .hasMessage(
                        "Minimum price cannot exceed maximum price"
                );

        verifyNoInteractions(listingRepository);
    }

    @Test
    void oversizedKeywordIsRejectedBeforeRepositoryCall() {
        ListingSearchCriteria criteria =
                new ListingSearchCriteria(
                        "a".repeat(
                                ListingSearchCriteria
                                        .MAXIMUM_KEYWORD_LENGTH
                                        + 1
                        ),
                        null,
                        null,
                        null,
                        null,
                        null
                );

        MarketplaceException exception =
                assertThrows(
                        MarketplaceException.class,
                        () -> listingService.searchActiveListings(
                                criteria,
                                0,
                                20
                        )
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        MarketplaceErrorCode.INVALID_LISTING_FILTERS
                );

        assertThat(exception)
                .hasMessage(
                        "Search keyword cannot exceed 100 characters"
                );

        verifyNoInteractions(listingRepository);
    }

    @Test
    void invalidPageSizeIsRejectedBeforeRepositoryCall() {
        ListingSearchCriteria criteria =
                new ListingSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        MarketplaceException exception =
                assertThrows(
                        MarketplaceException.class,
                        () -> listingService.searchActiveListings(
                                criteria,
                                0,
                                51
                        )
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        MarketplaceErrorCode.INVALID_LISTING_FILTERS
                );

        assertThat(exception)
                .hasMessage(
                        "Page size must be between 1 and 50"
                );

        verifyNoInteractions(listingRepository);
    }

    @Test
    void ownerCanWithdrawActiveListing() {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        Listing listing = newListing(sellerAccountId);

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        when(listingRepository.saveAndFlush(listing))
                .thenReturn(listing);

        ListingView result =
                listingService.changeListingStatus(
                        sellerAccountId,
                        listingId,
                        new ChangeListingStatusRequest(
                                ListingStatus.WITHDRAWN,
                                0L
                        )
                );

        assertThat(listing.getStatus())
                .isEqualTo(ListingStatus.WITHDRAWN);

        assertThat(result.status())
                .isEqualTo(ListingStatus.WITHDRAWN);

        verify(listingRepository)
                .saveAndFlush(listing);
    }

    @Test
    void repeatedCurrentStatusIsIdempotentEvenWithStaleVersion() {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        Listing listing = newListing(sellerAccountId);

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        ListingView result =
                listingService.changeListingStatus(
                        sellerAccountId,
                        listingId,
                        new ChangeListingStatusRequest(
                                ListingStatus.ACTIVE,
                                999L
                        )
                );

        assertThat(result.status())
                .isEqualTo(ListingStatus.ACTIVE);

        verify(listingRepository, never())
                .saveAndFlush(any(Listing.class));
    }

    @Test
    void nonOwnerCannotChangeListingStatus() {
        UUID ownerAccountId = UUID.randomUUID();
        UUID anotherAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        Listing listing = newListing(ownerAccountId);

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        MarketplaceException exception =
                assertThrows(
                        MarketplaceException.class,
                        () -> listingService.changeListingStatus(
                                anotherAccountId,
                                listingId,
                                new ChangeListingStatusRequest(
                                        ListingStatus.WITHDRAWN,
                                        0L
                                )
                        )
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        MarketplaceErrorCode
                                .LISTING_ACCESS_DENIED
                );

        verify(listingRepository, never())
                .saveAndFlush(any(Listing.class));
    }

    @Test
    void soldListingCannotTransitionDirectlyToWithdrawn() {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        Listing listing = newListing(sellerAccountId);
        listing.changeStatus(ListingStatus.SOLD);

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        MarketplaceException exception =
                assertThrows(
                        MarketplaceException.class,
                        () -> listingService.changeListingStatus(
                                sellerAccountId,
                                listingId,
                                new ChangeListingStatusRequest(
                                        ListingStatus.WITHDRAWN,
                                        0L
                                )
                        )
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        MarketplaceErrorCode
                                .INVALID_STATUS_TRANSITION
                );

        assertThat(listing.getStatus())
                .isEqualTo(ListingStatus.SOLD);

        verify(listingRepository, never())
                .saveAndFlush(any(Listing.class));
    }

    @Test
    void ownerCanUpdateListingDetails() {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        Listing listing = newListing(sellerAccountId);

        Category laptops = category(
                "electronics",
                "Electronics",
                "laptops",
                "Laptops"
        );

        Brand apple = new Brand(
                "apple",
                "Apple",
                10
        );

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        when(catalogSelectionResolver.resolveForCreate(
                "LAPTOPS",
                "APPLE"
        )).thenReturn(
                new ResolvedCatalogSelection(
                        laptops,
                        apple
                )
        );

        when(listingRepository.saveAndFlush(listing))
                .thenReturn(listing);

        UpdateListingRequest request =
                new UpdateListingRequest(
                        "LAPTOPS",
                        "APPLE",
                        "MacBook Pro",
                        "M4 MacBook Pro with original box",
                        2025,
                        ListingCondition.LIKE_NEW,
                        new BigDecimal("1499.99"),
                        "Main Campus",
                        DeliveryMethod.LOCAL_PICKUP,
                        List.of(
                                "https://images.example.com/macbook-front.jpg",
                                "https://images.example.com/macbook-back.jpg"
                        ),
                        0L
                );

        ListingView result =
                listingService.updateListing(
                        sellerAccountId,
                        listingId,
                        request
                );

        assertThat(result.title())
                .isEqualTo("MacBook Pro");

        assertThat(result.price())
                .isEqualByComparingTo("1499.99");

        assertThat(result.category().slug())
                .isEqualTo("laptops");

        assertThat(result.brand().slug())
                .isEqualTo("apple");

        assertThat(result.imageUrls())
                .containsExactly(
                        "https://images.example.com/macbook-front.jpg",
                        "https://images.example.com/macbook-back.jpg"
                );

        assertThat(result.status())
                .isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void optimisticLockFailureBecomesMarketplaceConflict() {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        Listing listing = newListing(sellerAccountId);

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        when(listingRepository.saveAndFlush(listing))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "Concurrent listing update"
                        )
                );

        MarketplaceException exception =
                assertThrows(
                        MarketplaceException.class,
                        () -> listingService.changeListingStatus(
                                sellerAccountId,
                                listingId,
                                new ChangeListingStatusRequest(
                                        ListingStatus.WITHDRAWN,
                                        0L
                                )
                        )
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        MarketplaceErrorCode
                                .LISTING_UPDATE_CONFLICT
                );
    }

    private Listing newListing(UUID sellerAccountId) {
        Category graphicsCards = category(
                "electronics",
                "Electronics",
                "graphics-cards",
                "Graphics Cards"
        );

        Brand nvidia = new Brand(
                "nvidia",
                "NVIDIA",
                10
        );

        return new Listing(
                sellerAccountId,
                graphicsCards,
                nvidia,
                "NVIDIA RTX 5090",
                "Original marketplace listing",
                2025,
                ListingCondition.NEW,
                new BigDecimal("1999.99"),
                "Main Campus",
                DeliveryMethod.LOCAL_PICKUP,
                List.of(
                        "https://images.example.com/rtx-5090.jpg"
                )
        );
    }

    private Category category(
            String parentSlug,
            String parentDisplayName,
            String childSlug,
            String childDisplayName
    ) {
        Category parent = new Category(
                parentSlug,
                parentDisplayName,
                null,
                10
        );

        return new Category(
                childSlug,
                childDisplayName,
                parent,
                10
        );
    }
}