package com.campushub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank
        @Size(max = MAXIMUM_TOKEN_LENGTH)
        String refreshToken
) {

        public static final int MAXIMUM_TOKEN_LENGTH = 512;
}