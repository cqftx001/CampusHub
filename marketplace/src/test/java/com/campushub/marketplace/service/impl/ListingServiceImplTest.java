package com.campushub.marketplace.service.impl;

import com.campushub.marketplace.domain.DeliveryMethod;
import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.dto.ListingSearchCriteria;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.mapper.ListingMapper;
import com.campushub.marketplace.repository.ListingRepository;
import com.campushub.marketplace.repository.projection.ListingSummaryRow;
import com.campushub.marketplace.service.CatalogSelectionResolver;
import com.campushub.marketplace.vo.ListingPageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
}