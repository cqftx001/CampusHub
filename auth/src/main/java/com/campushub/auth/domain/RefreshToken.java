package com.campushub.auth.domain;

import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.shared.base.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        schema = "auth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_auth_refresh_tokens_token_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(
                        name = "ix_auth_refresh_tokens_session_status",
                        columnList = "session_id, status"
                ),
                @Index(
                        name = "ix_auth_refresh_tokens_expires_at",
                        columnList = "expires_at"
                )
        }
)
public class RefreshToken extends BaseEntity {

    private static final int SHA_256_HEX_LENGTH = 64;

    @Column(
            name = "session_id",
            nullable = false,
            updatable = false
    )
    private UUID sessionId;

    @Column(
            name = "token_hash",
            nullable = false,
            updatable = false,
            length = SHA_256_HEX_LENGTH
    )
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RefreshTokenStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshToken() {
    }

    public RefreshToken(
            UUID sessionId,
            String tokenHash,
            Instant issueAt,
            Instant expiresAt
    ){
        this.sessionId = Objects.requireNonNull(sessionId);
        this.tokenHash = requireTokenHash(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);

        if(!expiresAt.isAfter(Objects.requireNonNull(issueAt))) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        this.status = RefreshTokenStatus.ACTIVE;
    }

    public boolean isUsable(Instant now){
        Objects.requireNonNull(now);

        return status == RefreshTokenStatus.ACTIVE && expiresAt.isAfter(now);
    }

    public void markUsed(Instant usedAt,
                         UUID replacementTokenId){
        Objects.requireNonNull(usedAt);
        Objects.requireNonNull(replacedByTokenId);

        if(!isUsable(usedAt)){
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NON_USABLE);
        }

        this.status  = RefreshTokenStatus.USED;
        this.usedAt = usedAt;
        this.replacedByTokenId = replacementTokenId;
    }

    public void revoke(Instant revokedAt){
        Objects.requireNonNull(revokedAt);

        if(status != RefreshTokenStatus.ACTIVE){
            return;
        }

        this.status = RefreshTokenStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public RefreshTokenStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedByTokenId() {
        return replacedByTokenId;
    }
    // --- helper ---
    private static String requireTokenHash(String tokenHash){
        String requiredHash = Objects.requireNonNull(tokenHash);

        if(requiredHash.length() != SHA_256_HEX_LENGTH){
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_LENGTH_INVALID);
        }

        return requiredHash;
    }
}
