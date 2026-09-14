package com.campushub.marketplace.mapper;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;
import com.campushub.marketplace.vo.BrandReferenceView;
import com.campushub.marketplace.vo.CategoryGroupView;
import com.campushub.marketplace.vo.CategoryOptionView;
import com.campushub.marketplace.vo.MarketplaceCatalogView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CatalogMapper {

    public MarketplaceCatalogView toView(
            List<Category> categories,
            List<Brand> brands
    ) {
        Map<UUID, List<Category>> childrenByParentId = categories
                .stream()
                .filter(Category::isLeaf)
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));

        List<CategoryGroupView> categoryGroups =
                categories.stream()
                        .filter(Category::isRoot)
                        .map(root -> toCategoryGroup(
                                root,
                                childrenByParentId.getOrDefault(
                                        root.getId(),
                                        List.of()
                                )
                        ))
                        .toList();


        List<BrandReferenceView> brandViews = brands
                .stream()
                .map(this::toBrandView)
                .toList();

        return new MarketplaceCatalogView(categoryGroups, brandViews);
    }

    // --- helper ---

    /**
     * 封装成一二级目录CategoryGroupView
     * slug / displayName / List<CategoryOptionView> children
     * @param root
     * @param children
     * @return
     */
    private CategoryGroupView toCategoryGroup(Category root, List<Category> children) {

        List<CategoryOptionView> childViews = children.stream()
                .map(child -> new CategoryOptionView(child.getSlug(), child.getDisplayName()))
                .toList();

        return new CategoryGroupView(
                root.getSlug(),
                root.getDisplayName(),
                childViews
        );
    }

    private BrandReferenceView toBrandView(Brand brand) {
        return new BrandReferenceView(brand.getSlug(), brand.getDisplayName());
    }

}
