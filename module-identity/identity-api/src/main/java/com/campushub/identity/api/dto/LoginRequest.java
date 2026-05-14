package com.campushub.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request")
public record LoginRequest(
        @NotBlank(message = "Email or Username is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {}
