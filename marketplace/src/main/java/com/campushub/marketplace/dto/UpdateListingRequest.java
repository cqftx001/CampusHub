package com.campushub.marketplace.dto;

import com.campushub.marketplace.domain.DeliveryMethod;
import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.utils.CatalogNormalizer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateListingRequest(

        @NotBlank(message = "Category is required")
        @Size(
                max = CatalogNormalizer.MAXIMUM_SLUG_LENGTH,
                message = "Category slug is too long"
        )
        @Pattern(
                regexp = "[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*",
                message = "Category slug is invalid"
        )
        String categorySlug,

        @Size(
                max = CatalogNormalizer.MAXIMUM_SLUG_LENGTH,
                message = "Brand slug is too long"
        )
        @Pattern(
                regexp = "[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*",
                message = "Brand slug is invalid"
        )
        String brandSlug,

        @NotBlank(message = "Title is required")
        @Size(
                max = Listing.MAXIMUM_TITLE_LENGTH,
                message = "Title is too long"
        )
        String title,

        @NotBlank(message = "Description is required")
        @Size(
                max = Listing.MAXIMUM_DESCRIPTION_LENGTH,
                message = "Description is too long"
        )
        String description,

        @Min(
                value = 1800,
                message = "Manufacture year must be 1800 or later"
        )
        @Max(
                value = 2100,
                message = "Manufacture year must not exceed 2100"
        )
        Integer manufactureYear,

        @NotNull(message = "Condition is required")
        ListingCondition condition,

        @NotNull(message = "Price is required")
        @DecimalMin(
                value = "0.01",
                message = "Price must be greater than zero"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Price supports up to 10 integer and 2 decimal digits"
        )
        BigDecimal price,

        @NotBlank(message = "Location is required")
        @Size(
                max = Listing.MAXIMUM_LOCATION_LENGTH,
                message = "Location is too long"
        )
        String location,

        @NotNull(message = "Delivery method is required")
        DeliveryMethod deliveryMethod,

        @NotEmpty(message = "At least one image is required")
        @Size(
                max = Listing.MAXIMUM_IMAGE_COUNT,
                message = "A listing can contain at most 8 images"
        )
        List<
                @NotBlank(message = "Image URL cannot be empty")
                @Size(
                        max = Listing.MAXIMUM_IMAGE_URL_LENGTH,
                        message = "Image URL is too long"
                )
                @Pattern(
                        regexp = "^https://\\S+$",
                        message = "Image URL must use HTTPS"
                )
                        String
                > imageUrls,

        @NotNull(message = "Expected version is required")
        @PositiveOrZero(
                message = "Expected version cannot be negative"
        )
        Long expectedVersion
) {
}