package com.campushub.app.marketplace;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;
import com.campushub.marketplace.domain.DeliveryMethod;
import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.repository.BrandRepository;
import com.campushub.marketplace.repository.CategoryRepository;
import com.campushub.marketplace.repository.ListingRepository;
import com.campushub.marketplace.repository.projection.ListingSummaryRow;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketplaceListingRepositoryIntegrationTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Category graphicsCards;
    private Category phones;
    private Category laptops;
    private Category furniture;

    private Brand nvidia;
    private Brand apple;

    @BeforeEach
    void setUp() {
        graphicsCards = category("graphics-cards");
        phones = category("phones");
        laptops = category("laptops");
        furniture = category("furniture");

        nvidia = brand("nvidia");
        apple = brand("apple");
    }

    @Test
    void rootAndLeafCategoryFiltersAreSupported() {
        saveListing(
                graphicsCards,
                nvidia,
                "NVIDIA RTX 5090",
                ListingCondition.NEW,
                "1999.99"
        );

        saveListing(
                phones,
                apple,
                "Apple iPhone 16 Pro",
                ListingCondition.LIKE_NEW,
                "899.00"
        );

        saveListing(
                furniture,
                null,
                "Study Desk",
                ListingCondition.GOOD,
                "80.00"
        );

        Page<ListingSummaryRow> electronics =
                search(
                        null,
                        "electronics",
                        null,
                        null,
                        null,
                        null,
                        0,
                        10
                );

        Assertions.assertThat(electronics.getContent())
                .extracting(ListingSummaryRow::title)
                .containsExactlyInAnyOrder(
                        "NVIDIA RTX 5090",
                        "Apple iPhone 16 Pro"
                );

        Page<ListingSummaryRow> graphicsCardResults =
                search(
                        null,
                        "graphics-cards",
                        null,
                        null,
                        null,
                        null,
                        0,
                        10
                );

        Assertions.assertThat(graphicsCardResults.getContent())
                .extracting(ListingSummaryRow::title)
                .containsExactly(
                        "NVIDIA RTX 5090"
                );
    }

    @Test
    void brandCanBeUsedWithoutCategory() {
        saveListing(
                graphicsCards,
                nvidia,
                "NVIDIA RTX 5090",
                ListingCondition.NEW,
                "1999.99"
        );

        saveListing(
                laptops,
                nvidia,
                "NVIDIA Studio Laptop",
                ListingCondition.GOOD,
                "1200.00"
        );

        saveListing(
                phones,
                apple,
                "Apple iPhone 16 Pro",
                ListingCondition.LIKE_NEW,
                "899.00"
        );

        Page<ListingSummaryRow> result =
                search(
                        null,
                        null,
                        "nvidia",
                        null,
                        null,
                        null,
                        0,
                        10
                );

        Assertions.assertThat(result.getContent())
                .extracting(ListingSummaryRow::title)
                .containsExactlyInAnyOrder(
                        "NVIDIA RTX 5090",
                        "NVIDIA Studio Laptop"
                );
    }

    @Test
    void keywordConditionAndPriceCanBeCombined() {
        saveListing(
                graphicsCards,
                nvidia,
                "NVIDIA RTX 5090 Founders Edition",
                ListingCondition.LIKE_NEW,
                "1999.99"
        );

        saveListing(
                graphicsCards,
                nvidia,
                "NVIDIA RTX 4090",
                ListingCondition.GOOD,
                "1200.00"
        );

        Page<ListingSummaryRow> result =
                search(
                        "%rtx%5090%",
                        "electronics",
                        "nvidia",
                        ListingCondition.LIKE_NEW,
                        new BigDecimal("1900.00"),
                        new BigDecimal("2100.00"),
                        0,
                        10
                );

        Assertions.assertThat(result.getContent())
                .extracting(ListingSummaryRow::title)
                .containsExactly(
                        "NVIDIA RTX 5090 Founders Edition"
                );
    }

    @Test
    void withdrawnListingIsExcludedAndPaginationIsReported() {
        saveListing(
                phones,
                apple,
                "Active iPhone",
                ListingCondition.GOOD,
                "500.00"
        );

        Listing withdrawn = saveListing(
                phones,
                apple,
                "Withdrawn iPhone",
                ListingCondition.FAIR,
                "300.00"
        );

        jdbcTemplate.update(
                """
                update marketplace.listings
                set status = 'WITHDRAWN'
                where id = ?
                """,
                withdrawn.getId()
        );

        entityManager.clear();

        Page<ListingSummaryRow> result =
                search(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        1
                );

        Assertions.assertThat(result.getContent())
                .extracting(ListingSummaryRow::title)
                .containsExactly(
                        "Active iPhone"
                );

        Assertions.assertThat(result.getTotalElements())
                .isEqualTo(1);

        Assertions.assertThat(result.getTotalPages())
                .isEqualTo(1);

        Assertions.assertThat(result.hasNext())
                .isFalse();
    }

    private Page<ListingSummaryRow> search(
            String keywordPattern,
            String categorySlug,
            String brandSlug,
            ListingCondition condition,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            int page,
            int size
    ) {
        return listingRepository.searchActiveListingSummaries(
                ListingStatus.ACTIVE,
                keywordPattern,
                categorySlug,
                brandSlug,
                condition,
                minimumPrice,
                maximumPrice,
                PageRequest.of(page, size)
        );
    }

    private Listing saveListing(
            Category category,
            Brand brand,
            String title,
            ListingCondition condition,
            String price
    ) {
        Listing listing = new Listing(
                UUID.randomUUID(),
                category,
                brand,
                title,
                "Marketplace integration test listing",
                2025,
                condition,
                new BigDecimal(price),
                "Los Angeles, CA",
                DeliveryMethod.LOCAL_PICKUP,
                List.of(
                        "https://images.example.com/"
                                + UUID.randomUUID()
                                + ".jpg"
                )
        );

        return listingRepository.saveAndFlush(listing);
    }

    private Category category(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow();
    }

    private Brand brand(String slug) {
        return brandRepository.findBySlug(slug)
                .orElseThrow();
    }
}