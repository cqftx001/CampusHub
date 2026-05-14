package com.campushub.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest
 * @param email
 * @param password
 * @param displayName
 * @param agreeToTerms
 * @param agreeToPrivacyPolicy
 */

@Schema(description = "Register request")
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        String username,

        @NotBlank(message = "Display Name is required")
        @Size(max = 50)
        String displayName,

        @NotNull(message = "Terms agreement is required")
        @AssertTrue(message = "You must agree to the terms and conditions")
        Boolean agreeToTerms,

        @NotNull(message = "Privacy policy agreement is required")
        @AssertTrue(message = "You must agree to the privacy policy")
        Boolean agreeToPrivacyPolicy
) {
}
