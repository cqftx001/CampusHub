package com.campushub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(

        @NotBlank
        @Size(max = 72)
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)"
                        + "(?=.*[!@#$%^&*._-])"
                        + "[A-Za-z\\d!@#$%^&*._-]+$",
                message = "password must contain uppercase, lowercase, "
                        + "numeric, and special characters"
        )
        String newPassword
) {
}
