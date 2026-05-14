package com.campushub.identity.api.vo;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String accessToken,
        String tokenType,
        long expiresIn,
        String roles
){
    public static AuthResponse bearer(UUID userId, String accessToken, long expiresIn, String roles){
        return new AuthResponse(userId, accessToken, "Bearer", expiresIn, roles);
    }
}
