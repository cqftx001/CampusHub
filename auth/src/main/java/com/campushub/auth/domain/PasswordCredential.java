package com.campushub.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
     name = "local_credentials",
     schema = "auth"
)
public class PasswordCredential {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(
            name = "password_changed_at",
            nullable = false
    )
    private Instant passwordChangedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PasswordCredential() {
    }

    public PasswordCredential(
            UUID accountId,
            String passwordHash,
            Instant passwordChangedAt
    ) {
        this.accountId = Objects.requireNonNull(accountId);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.passwordChangedAt =
                Objects.requireNonNull(passwordChangedAt);
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public long getVersion() {
        return version;
    }
}
