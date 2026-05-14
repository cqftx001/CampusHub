package com.campushub.identity.impl.service;

import com.campushub.identity.api.vo.AuthResponse;

public record TokenPair(
        AuthResponse authResponse,
        String refreshToken
) {
}
