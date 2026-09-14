package com.campushub.marketplace.mapper;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;
import com.campushub.marketplace.vo.CategoryGroupView;
import com.campushub.marketplace.vo.MarketplaceCatalogView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogMapperTest {

    private final CatalogMapper catalogMapper =
            new CatalogMapper();

    @Test
    void categoriesAreGroupedIntoTwoLevelTree() {
        Category electronics = rootCategory(
                "electronics",
                "Electronics"
        );

        Category homeAndLiving = rootCategory(
                "home-living",
                "Home & Living"
        );

        Category graphicCards = childCategory(
                "graphic-cards",
                "Graphic Cards",
                electronics
        );

        Category phones = childCategory(
                "phones",
                "Phones",
                electronics
        );

        Category furniture = childCategory(
                "furniture",
                "Furniture",
                homeAndLiving
        );

        Brand nvidia = brand(
                "nvidia",
                "NVIDIA"
        );

        Brand apple = brand(
                "apple",
                "Apple"
        );

        MarketplaceCatalogView result =
                catalogMapper.toView(
                        List.of(
                                electronics,
                                graphicCards,
                                phones,
                                homeAndLiving,
                                furniture
                        ),
                        List.of(
                                nvidia,
                                apple
                        )
                );

        assertThat(result.categories())
                .extracting(CategoryGroupView::slug)
                .containsExactly(
                        "electronics",
                        "home-living"
                );

        assertThat(result.categories().getFirst().children())
                .extracting(child -> child.slug())
                .containsExactly(
                        "graphic-cards",
                        "phones"
                );

        assertThat(result.categories().get(1).children())
                .extracting(child -> child.slug())
                .containsExactly(
                        "furniture"
                );

        assertThat(result.brands())
                .extracting(brand -> brand.slug())
                .containsExactly(
                        "nvidia",
                        "apple"
                );
    }

    @Test
    void rootWithoutChildrenIsStillReturned() {
        Category luxury = rootCategory(
                "luxury",
                "Luxury"
        );

        MarketplaceCatalogView result =
                catalogMapper.toView(
                        List.of(luxury),
                        List.of()
                );

        assertThat(result.categories())
                .hasSize(1);

        assertThat(result.categories().getFirst().slug())
                .isEqualTo("luxury");

        assertThat(result.categories().getFirst().children())
                .isEmpty();
    }

    private Category rootCategory(
            String slug,
            String displayName
    ) {
        Category category = mock(Category.class);

        when(category.getId())
                .thenReturn(UUID.randomUUID());
        when(category.getSlug())
                .thenReturn(slug);
        when(category.getDisplayName())
                .thenReturn(displayName);
        when(category.isRoot())
                .thenReturn(true);
        when(category.isLeaf())
                .thenReturn(false);

        return category;
    }

    private Category childCategory(
            String slug,
            String displayName,
            Category parent
    ) {
        Category category = mock(Category.class);

        when(category.getId())
                .thenReturn(UUID.randomUUID());
        when(category.getSlug())
                .thenReturn(slug);
        when(category.getDisplayName())
                .thenReturn(displayName);
        when(category.getParent())
                .thenReturn(parent);
        when(category.isRoot())
                .thenReturn(false);
        when(category.isLeaf())
                .thenReturn(true);

        return category;
    }

    private Brand brand(
            String slug,
            String displayName
    ) {
        Brand brand = mock(Brand.class);

        when(brand.getSlug())
                .thenReturn(slug);
        when(brand.getDisplayName())
                .thenReturn(displayName);

        return brand;
    }
}