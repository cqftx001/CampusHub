package com.campushub.marketplace.service;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;

import java.util.Objects;

public record ResolvedCatalogSelection(
        Category category,
        Brand brand
){
    public ResolvedCatalogSelection {
        Objects.requireNonNull(category, "Category cannot be null");
    }
}
