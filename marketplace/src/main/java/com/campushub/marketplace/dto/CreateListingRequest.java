package com.campushub.marketplace.dto;

import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.utils.LabelSlugNormalizer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateListingRequest(

        @NotBlank(
                message = "Title is required"
        )
        @Size(
                max = 120,
                message = "Title must be at most 120 characters"
        )
        String title,

        @NotBlank(
                message = "Description is required"
        )
        @Size(
                max = 4000,
                message =
                        "Description must be at most 4000 characters"
        )
        String description,

        @Min(
                value = 1900,
                message =
                        "Manufacture year must not be before 1900"
        )
        Integer manufactureYear,

        @NotNull(
                message = "Condition is required"
        )
        ListingCondition condition,

        @NotNull(
                message = "Asking price is required"
        )
        @DecimalMin(
                value = "0.01",
                message =
                        "Asking price must be greater than zero"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message =
                        "Asking price must have at most "
                                + "10 integer digits and 2 decimal digits"
        )
        BigDecimal askingPrice,

        @NotBlank(
                message = "Location is required"
        )
        @Size(
                max = 120,
                message =
                        "Location must be at most 120 characters"
        )
        String location,

        @Size(
                max = 2048,
                message =
                        "Cover image URL must be at most 2048 characters"
        )
        @Pattern(
                regexp = "^https://\\S+$",
                message = "Cover image URL must use HTTPS"
        )
        String coverImageUrl,

        @NotBlank(
                message = "Category is required"
        )
        @Size(
                max = 64,
                message = "Category must be at most 64 characters"
        )

        @NotBlank(message = "Category is required")
        @Size(
                max = LabelSlugNormalizer.MAXIMUM_LENGTH,
                message = "Category must be at most 64 characters"
        )
        String category,

        @NotEmpty(message = "At least one child label is required")
        @Size(
                max = 3,
                message = "At most three child labels can be selected"
        )
        List<
                @NotBlank(message = "Label must not be blank")
                @Size(
                        max = LabelSlugNormalizer.MAXIMUM_LENGTH,
                        message = "Label must be at most 64 characters"
                )
                        String
                > labels
) {
}