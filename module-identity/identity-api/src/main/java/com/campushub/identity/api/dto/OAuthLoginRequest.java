package com.campushub.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "OAuth login request")
public record OAuthLoginRequest(
        @NotBlank(message = "Provider is required")
        String provider,

        @NotBlank(message = "Authorization code is required")
        String authorizationCode,

        @NotBlank(message = "Redirect URI is required")
        String redirectUri,

        String state
) {
}
