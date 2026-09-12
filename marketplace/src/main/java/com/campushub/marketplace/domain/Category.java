package com.campushub.marketplace.domain;

import com.campushub.marketplace.utils.CatalogNormalizer;
import com.campushub.shared.base.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "categories",
        schema = "marketplace",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_marketplace_categories_slug",
                columnNames = "slug"
        )
)
public class Category extends BaseEntity {

    /**
     * Slug(String), displayName(String), parent(Category), active(boolean), disPlayOrder(int)
     */


    @Column(
            nullable = false,
            length = CatalogNormalizer.MAXIMUM_SLUG_LENGTH,
            updatable = false
    )
    private String slug;

    @Column(
            name = "display_name",
            nullable = false,
            length = CatalogNormalizer.MAXIMUM_DISPLAY_NAME_LENGTH
    )
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_marketplace_categories_parent"
            )
    )
    private Category parent;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Category() {}

    public Category(String slug, String displayName, Category parent, int displayOrder) {
        if (parent != null && !parent.isRoot()) {
            throw new IllegalArgumentException("Marketplace categories support only two levels");
        }

        this.slug = CatalogNormalizer.normalizeSlug(slug);
        this.displayName = CatalogNormalizer.normalizeDisplayName(displayName);
        this.displayOrder = CatalogNormalizer.requireDisplayOrder(displayOrder);
        this.parent = parent;
        this.active = true;
    }

    public boolean isRoot() {
        return parent == null;
    }

    // Check if the category is allowed to be selected when publishing a product
    public boolean isAvailableForListing() {
        return active && parent != null && parent.isRoot() && parent.isActive();
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Category getParent() {
        return parent;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

}
