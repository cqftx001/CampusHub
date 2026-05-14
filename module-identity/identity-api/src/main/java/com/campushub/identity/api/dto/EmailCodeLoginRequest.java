package com.campushub.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailCodeLoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 10, message = "Verification code must be 6-10 characters")
        String code
) {
}
