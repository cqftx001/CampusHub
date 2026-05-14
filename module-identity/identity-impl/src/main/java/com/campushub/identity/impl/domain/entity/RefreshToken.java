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

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", length = 100)
    private String revokeReason;


    private RefreshToken(
            UUID userId,
            UUID sessionId,
            UUID tokenFamilyId,
            String tokenHash,
            Instant expiresAt
    ) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.tokenFamilyId = tokenFamilyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }


    public static RefreshToken issue(
            UUID userId,
            UUID sessionId,
            UUID tokenFamilyId,
            String tokenHash,
            Instant expiresAt
    ) {
        if (userId == null) throw new IllegalArgumentException("User id must not be null");
        if (sessionId == null) throw new IllegalArgumentException("Session id must not be null");
        if (tokenFamilyId == null) throw new IllegalArgumentException("Token family id must not be null");
        if (tokenHash == null || tokenHash.isBlank()) throw new IllegalArgumentException("Token hash must not be blank");
        if (expiresAt == null) throw new IllegalArgumentException("Expiration time must not be null");

        return new RefreshToken(userId, sessionId, tokenFamilyId, tokenHash, expiresAt);
    }

    /**
     * 作废 token（退出登录、改密码时调用）。
     */
    public void revoke(String reason) {
        if(this.revoked) return;
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revokeReason = reason;
    }

    /**
     * 判断 token 是否有效（未过期 + 未被作废）。
     */
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }
}
