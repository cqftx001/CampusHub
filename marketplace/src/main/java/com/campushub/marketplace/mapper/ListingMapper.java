package com.campushub.marketplace.mapper;

import com.campushub.marketplace.domain.Brand;
import com.campushub.marketplace.domain.Category;
import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.repository.projection.ListingSummaryRow;
import com.campushub.marketplace.repository.projection.SellerListingSummaryRow;
import com.campushub.marketplace.vo.*;
import org.springframework.stereotype.Component;

@Component
public class ListingMapper {

    public ListingView toView(Listing listing) {
        return new ListingView(
                listing.getId(),
                listing.getSellerAccountId(),
                toCategoryView(listing.getCategory()),
                toBrandView(listing.getBrand()),
                listing.getTitle(),
                listing.getDescription(),
                listing.getManufactureYear(),
                listing.getCondition(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getLocation(),
                listing.getDeliveryMethod(),
                listing.getImageUrls(),
                listing.getStatus(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }

    public ListingSummaryView toSummaryView(
            ListingSummaryRow row
    ) {
        return new ListingSummaryView(
                row.id(),
                new CategoryReferenceView(
                        row.categorySlug(),
                        row.categoryDisplayName(),
                        row.parentCategorySlug(),
                        row.parentCategoryDisplayName()
                ),
                toBrandView(row),
                row.title(),
                row.condition(),
                row.price(),
                row.currency(),
                row.location(),
                row.deliveryMethod(),
                row.primaryImageUrl(),
                row.createdAt()
        );
    }

    public SellerListingSummaryView toSellerSummaryView(
            SellerListingSummaryRow row
    ) {
        return new SellerListingSummaryView(
                row.id(),
                row.title(),
                row.price(),
                row.currency(),
                row.primaryImageUrl(),
                row.status(),
                row.version(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private CategoryReferenceView toCategoryView(
            Category category
    ) {
        Category parent = category.getParent();

        return new CategoryReferenceView(
                category.getSlug(),
                category.getDisplayName(),
                parent.getSlug(),
                parent.getDisplayName()
        );
    }

    private BrandReferenceView toBrandView(Brand brand) {
        if (brand == null) {
            return null;
        }

        return new BrandReferenceView(
                brand.getSlug(),
                brand.getDisplayName()
        );
    }

    private BrandReferenceView toBrandView(
            ListingSummaryRow row
    ) {
        if (row.brandSlug() == null) {
            return null;
        }

        return new BrandReferenceView(
                row.brandSlug(),
                row.brandDisplayName()
        );
    }
}