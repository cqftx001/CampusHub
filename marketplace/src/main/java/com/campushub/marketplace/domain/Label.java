package com.campushub.marketplace.domain;

import com.campushub.marketplace.utils.CatalogNormalizer;
import com.campushub.marketplace.utils.LabelSlugNormalizer;
import com.campushub.shared.base.BaseEntity;
import com.campushub.shared.utils.TextNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(
        name = "labels",
        schema = "marketplace",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_marketplace_labels_slug",
                columnNames = "slug"
        )
)
public class Label extends BaseEntity {

    /**
     * slug(String), displayName(String), parent(Label), active(boolean), disPlayOrder(int)
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

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Label() {
    }

    public Label(
            String slug,
            String displayName,
            int displayOrder
    ) {
        this.slug = CatalogNormalizer.normalizeSlug(slug);
        this.displayName =
                CatalogNormalizer.normalizeDisplayName(displayName);
        this.displayOrder =
                CatalogNormalizer.requireDisplayOrder(displayOrder);
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