package com.campushub.marketplace.service;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.repository.BrandRepository;
import com.campushub.marketplace.repository.CategoryRepository;
import com.campushub.marketplace.utils.CatalogNormalizer;
import com.campushub.shared.utils.TextNormalizer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CatalogSelectionResolver {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public CatalogSelectionResolver(CategoryRepository categoryRepository, BrandRepository brandRepository) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    public ResolvedCatalogSelection resolveForCreate(
            String categorySlug,
            String brandSlug
    ) {
        Category category = resolveListingCategory(categorySlug);
        Brand brand = resolveOptionalBrand(brandSlug);

        return new ResolvedCatalogSelection(category, brand);
    }

    private Category resolveListingCategory(String categorySlug) {
        String normalizedSlug = CatalogNormalizer.normalizeSlug(categorySlug);

        Category category = categoryRepository
                .findBySlug(normalizedSlug)
                .orElseThrow(() -> new MarketplaceException(
                        MarketplaceErrorCode.CATEGORY_NOT_FOUND
                ));

        if (!category.isAvailableForListing()) {
            throw new MarketplaceException(
                    MarketplaceErrorCode.CATEGORY_NOT_AVAILABLE
            );
        }

        return category;
    }

    private Brand resolveOptionalBrand(String brandSlug) {
        String strippedSlug = TextNormalizer.stripToNull(brandSlug);

        if (strippedSlug == null) {
            return null;
        }

        String normalizedSlug = CatalogNormalizer.normalizeSlug(strippedSlug);

        Brand brand = brandRepository
                .findBySlug(normalizedSlug)
                .orElseThrow(() -> new MarketplaceException(
                        MarketplaceErrorCode.BRAND_NOT_FOUND
                ));

        if (!brand.isActive()) {
            throw new MarketplaceException(
                    MarketplaceErrorCode.BRAND_NOT_AVAILABLE
            );
        }

        return brand;
    }
}
