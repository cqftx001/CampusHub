package com.campushub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Recipient name is required")
        @Size(
                max = 100,
                message = "Recipient name must be at most 100 characters"
        )
        String recipientName,

        @NotBlank(message = "Address line 1 is required")
        @Size(
                max = 120,
                message = "Address line 1 must be at most 120 characters"
        )
        String line1,

        @Size(
                max = 120,
                message = "Address line 2 must be at most 120 characters"
        )
        String line2,

        @NotBlank(message = "City is required")
        @Size(
                max = 100,
                message = "City must be at most 100 characters"
        )
        String city,

        @NotBlank(message = "State is required")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "State must be a two-letter US state code"
        )
        String state,

        @NotBlank(message = "Postal code is required")
        @Pattern(
                regexp = "^\\d{5}(-\\d{4})?$",
                message = "Postal code must be ZIP or ZIP+4 format"
        )
        String postalCode
) {

    public String countryCode() {
        return "US";
    }
}