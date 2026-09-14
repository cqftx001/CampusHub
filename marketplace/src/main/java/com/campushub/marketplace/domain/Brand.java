package com.campushub.marketplace.domain;

import com.campushub.marketplace.utils.CatalogNormalizer;
import com.campushub.shared.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "brands",
        schema = "marketplace",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_marketplace_brands_slug",
                columnNames = "slug"
        )
)
public class Brand extends BaseEntity {

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

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Brand() {
    }

    public Brand(
            String slug,
            String displayName,
            int displayOrder
    ) {
        this.slug = CatalogNormalizer.normalizeSlug(slug);
        this.displayName = CatalogNormalizer.normalizeDisplayName(displayName);
        this.displayOrder = CatalogNormalizer.requireDisplayOrder(displayOrder);
        this.active = true;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

}
