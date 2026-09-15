package com.campushub.marketplace.domain;

import com.campushub.shared.base.BaseEntity;
import com.campushub.shared.utils.TextNormalizer;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "listings",
        schema = "marketplace"
)
public class Listing extends BaseEntity {

    public static final int MAXIMUM_TITLE_LENGTH = 160;
    public static final int MAXIMUM_DESCRIPTION_LENGTH = 5_000;
    public static final int MAXIMUM_LOCATION_LENGTH = 160;
    public static final int MAXIMUM_IMAGE_URL_LENGTH = 2_048;
    public static final int MAXIMUM_IMAGE_COUNT = 8;

    @Column(
            name = "seller_account_id",
            nullable = false,
            updatable = false
    )
    private UUID sellerAccountId;

    @Column(
            nullable = false,
            length = MAXIMUM_TITLE_LENGTH
    )
    private String title;

    @Column(
            nullable = false,
            length = MAXIMUM_DESCRIPTION_LENGTH
    )
    private String description;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 32
    )
    private ListingCondition condition;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal price;

    @Column(
            nullable = false,
            length = 3,
            updatable = false
    )
    private String currency;

    @Column(
            nullable = false,
            length = MAXIMUM_LOCATION_LENGTH
    )
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "delivery_method",
            nullable = false,
            length = 32
    )
    private DeliveryMethod deliveryMethod;

    @Column(
            name = "primary_image_url",
            nullable = false,
            length = MAXIMUM_IMAGE_URL_LENGTH
    )
    private String primaryImageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "listing_additional_images",
            schema = "marketplace",
            joinColumns = @JoinColumn(name = "listing_id"),
            foreignKey = @ForeignKey(
                    name = "fk_marketplace_additional_images_listing"
            )
    )
    @OrderColumn(name = "display_order")
    @Column(
            name = "image_url",
            nullable = false,
            length = MAXIMUM_IMAGE_URL_LENGTH
    )
    private List<String> additionalImageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 32
    )
    private ListingStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_marketplace_listings_category"
            )
    )
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "brand_id",
            foreignKey = @ForeignKey(
                    name = "fk_marketplace_listings_brand"
            )
    )
    private Brand brand;

    protected Listing() {
    }

    public Listing(
            UUID sellerAccountId,
            Category category,
            Brand brand,
            String title,
            String description,
            Integer manufactureYear,
            ListingCondition condition,
            BigDecimal price,
            String location,
            DeliveryMethod deliveryMethod,
            List<String> imageUrls
    ) {
        this.sellerAccountId =
                Objects.requireNonNull(
                        sellerAccountId,
                        "Seller account ID cannot be null"
                );

        this.category = requireListingCategory(category);
        this.brand = requireActiveBrand(brand);

        this.title = requireText(
                title,
                MAXIMUM_TITLE_LENGTH,
                "Title"
        );

        this.description = requireText(
                description,
                MAXIMUM_DESCRIPTION_LENGTH,
                "Description"
        );

        this.manufactureYear = requireManufactureYear(manufactureYear);

        this.condition = Objects.requireNonNull(
                condition,
                "Condition cannot be null"
        );

        this.price = requirePrice(price);
        this.currency = "USD";

        this.location = requireText(location, MAXIMUM_LOCATION_LENGTH, "Location");

        this.deliveryMethod = Objects.requireNonNull(
                deliveryMethod,
                "Delivery method cannot be null"
        );

        List<String> normalizedImages = requireImages(imageUrls);

        this.primaryImageUrl = normalizedImages.getFirst();

        this.additionalImageUrls
                .addAll(normalizedImages.subList(1, normalizedImages.size()));
        this.status = ListingStatus.ACTIVE;
    }

    public void updateDetails(
            Category category,
            Brand brand,
            String title,
            String description,
            Integer manufactureYear,
            ListingCondition condition,
            BigDecimal price,
            String location,
            DeliveryMethod deliveryMethod,
            List<String> imageUrls
    ) {
        this.category = requireListingCategory(category);
        this.brand = requireActiveBrand(brand);

        this.title = requireText(title, MAXIMUM_TITLE_LENGTH, "Title");

        this.description = requireText(description, MAXIMUM_DESCRIPTION_LENGTH, "Description");

        this.manufactureYear = requireManufactureYear(manufactureYear);

        this.condition =
                Objects.requireNonNull(
                        condition,
                        "Condition cannot be null"
                );

        this.price = requirePrice(price);

        this.location = requireText(location, MAXIMUM_LOCATION_LENGTH, "Location");

        this.deliveryMethod =
                Objects.requireNonNull(
                        deliveryMethod,
                        "Delivery method cannot be null"
                );

        List<String> normalizedImages = requireImages(imageUrls);

        this.primaryImageUrl = normalizedImages.getFirst();

        this.additionalImageUrls.clear();
        this.additionalImageUrls.addAll(normalizedImages.subList(1, normalizedImages.size()));
    }

    public boolean changeStatus(ListingStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "Target status cannot be null");

        if (status == targetStatus) return false;

        boolean allowed = status == ListingStatus.ACTIVE
                && (targetStatus == ListingStatus.SOLD || targetStatus == ListingStatus.WITHDRAWN)
                        ||
                (status == ListingStatus.SOLD || status == ListingStatus.WITHDRAWN)
                        && targetStatus == ListingStatus.ACTIVE;

        if (!allowed) {
            throw new MarketplaceException(MarketplaceErrorCode.INVALID_STATUS_TRANSITION);
        }

        status = targetStatus;
        return true;
    }

    public UUID getSellerAccountId() {
        return sellerAccountId;
    }

    public Category getCategory() {
        return category;
    }

    public Brand getBrand() {
        return brand;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getManufactureYear() {
        return manufactureYear;
    }

    public ListingCondition getCondition() {
        return condition;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getLocation() {
        return location;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }

    public List<String> getImageUrls() {
        List<String> images =
                new ArrayList<>(1 + additionalImageUrls.size());

        images.add(primaryImageUrl);
        images.addAll(additionalImageUrls);

        return List.copyOf(images);
    }

    public ListingStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == ListingStatus.ACTIVE;
    }

    // --- helper ---
    private static Integer requireManufactureYear(Integer manufactureYear) {
        if (manufactureYear == null) return null;

        if (manufactureYear < 1800 || manufactureYear > 2100) {
            throw new IllegalArgumentException("Manufacture year must be between 1800 and 2100");
        }

        return manufactureYear;
    }

    private static Category requireListingCategory(
            Category category
    ) {
        Objects.requireNonNull(
                category,
                "Category cannot be null"
        );

        if (!category.isAvailableForListing()) {
            throw new IllegalArgumentException(
                    "Listing must use an active second-level category"
            );
        }

        return category;
    }

    private static Brand requireActiveBrand(Brand brand) {
        if (brand != null && !brand.isActive()) {
            throw new IllegalArgumentException(
                    "Brand must be active"
            );
        }

        return brand;
    }

    private static String requireText(
            String value,
            int maximumLength,
            String fieldName
    ) {
        String normalized = TextNormalizer.stripToNull(value);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be empty"
            );
        }

        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName + " exceeds maximum length"
            );
        }

        return normalized;
    }

    private static BigDecimal requirePrice(BigDecimal value) {
        Objects.requireNonNull(
                value,
                "Price cannot be null"
        );

        if (value.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Price must be greater than zero"
            );
        }

        try {
            BigDecimal normalized =
                    value.setScale(2, RoundingMode.UNNECESSARY);

            if (normalized.precision() > 12) {
                throw new IllegalArgumentException(
                        "Price exceeds supported precision"
                );
            }

            return normalized;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Price supports at most two decimal places",
                    exception
            );
        }
    }

    private static List<String> requireImages(
            List<String> imageUrls
    ) {
        if (imageUrls == null
                || imageUrls.isEmpty()
                || imageUrls.size() > MAXIMUM_IMAGE_COUNT) {
            throw new IllegalArgumentException(
                    "Listing must contain between 1 and 8 images"
            );
        }

        List<String> normalized = imageUrls.stream()
                .map(TextNormalizer::stripToNull)
                .toList();

        if (normalized.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Image URL cannot be empty"
            );
        }

        if (normalized.stream().anyMatch(
                url -> url.length() > MAXIMUM_IMAGE_URL_LENGTH
        )) {
            throw new IllegalArgumentException(
                    "Image URL exceeds maximum length"
            );
        }

        if (normalized.stream().distinct().count()
                != normalized.size()) {
            throw new IllegalArgumentException(
                    "Image URLs cannot be duplicated"
            );
        }

        return normalized;
    }
}