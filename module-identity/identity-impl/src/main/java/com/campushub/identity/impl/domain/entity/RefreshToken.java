package com.campushub.identity.impl.domain.entity;

import com.campushub.infrastructure.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Database + Redis Hash
 */
@Getter
@Entity
@Table(name = "refresh_tokens", schema = "identity")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    private RefreshToken(UUID userId, String token, Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public static RefreshToken issue(UUID userId, String token, Instant expiresAt) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration time must not be null");
        }
        return new RefreshToken(userId, token, expiresAt);
    }

    /**
     * 作废 token（退出登录、改密码时调用）。
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * 判断 token 是否有效（未过期 + 未被作废）。
     */
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }
}
