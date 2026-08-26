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
        name = "login_sessions",
        schema = "auth",
        indexes = {
                @Index(
                        name = "ix_auth_login_sessions_account_id",
                        columnList = "account_id"
                ),
                @Index(
                        name = "ix_auth_login_sessions_status_expires_at",
                        columnList = "status, expires_at"
                )
        }
)
public class LoginSession extends BaseEntity {

    @Column(
            name = "account_id",
            nullable = false,
            updatable = false

    )
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LoginSessionStatus status;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    protected LoginSession() {
    }

    public LoginSession(
            UUID accountId,
            Instant startedAt,
            Instant expiresAt,
            String userAgent,
            String ipAddress
    ) {
        this.accountId = Objects.requireNonNull(accountId);
        this.lastUsedAt = Objects.requireNonNull(startedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);

        if (!expiresAt.isAfter(startedAt)) {
            throw new IllegalArgumentException(
                    "Session expiration must be after its start time."
            );
        }

        this.status = LoginSessionStatus.ACTIVE;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public boolean isActive(Instant now) {
        Objects.requireNonNull(now);

        return status == LoginSessionStatus.ACTIVE && expiresAt.isAfter(now);
    }

    public void markUsed(Instant usedAt){
        Objects.requireNonNull(usedAt);

        if(status != LoginSessionStatus.ACTIVE){
            throw new AuthException(AuthErrorCode.SESSION_ALREADY_REVOKED);
        }

        if(!usedAt.isBefore(expiresAt)){
            throw new AuthException(AuthErrorCode.SESSION_ALREADY_EXPIRED);
        }

        if(usedAt.isAfter(lastUsedAt)){
            this.lastUsedAt = usedAt;
        }
    }

    public void revoke(Instant revoke){
        Objects.requireNonNull(revoke);

        if(status == LoginSessionStatus.REVOKED){
            return;
        }

        this.status = LoginSessionStatus.REVOKED;
        this.revokedAt = revoke;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public LoginSessionStatus getStatus() {
        return status;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
