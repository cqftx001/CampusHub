package com.campushub.auth.token;

import com.campushub.auth.domain.RefreshToken;

import java.util.Objects;

/**
 * 含Raw Token, 发送给浏览器
 * @param value
 * @param refreshToken
 */
public record IssuedRefreshToken(
        String value,
        RefreshToken refreshToken
) {
    public IssuedRefreshToken(String value, RefreshToken refreshToken) {
        this.value = Objects.requireNonNull(value);
        this.refreshToken = Objects.requireNonNull(refreshToken);
    }
}
