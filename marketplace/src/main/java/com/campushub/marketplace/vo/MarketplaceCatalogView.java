package com.campushub.marketplace.vo;

import java.util.List;

public record MarketplaceCatalogView(
        List<CategoryGroupView> categories,
        List<BrandReferenceView> brands
) {

    public MarketplaceCatalogView(List<CategoryGroupView> categories, List<BrandReferenceView> brands) {
        this.categories = categories;
        this.brands = brands;
    }
}
