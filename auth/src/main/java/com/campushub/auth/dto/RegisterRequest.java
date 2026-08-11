package com.campushub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "username may contain only letters, numbers, dots, underscores, and hyphens"
        )
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*._-])[A-Za-z\\d!@#$%^&*._-]+$",
                message = "password must contain uppercase, lowercase, numeric, and special characters"
        )
        String password
) {
}
