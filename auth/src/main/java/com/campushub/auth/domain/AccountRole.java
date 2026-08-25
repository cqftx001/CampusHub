package com.campushub.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "account_roles",
        schema = "auth"
)
public class AccountRole {

    @EmbeddedId
    private AccountRoleId id;

    @Column(
            name = "assigned_at",
            nullable = false,
            updatable = false
    )
    private Instant assignedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected AccountRole() {
    }

    public AccountRole(
            UUID accountId,
            UUID roleId,
            Instant assignedAt
    ) {
        this.id = new AccountRoleId(accountId, roleId);
        this.assignedAt = Objects.requireNonNull(assignedAt);
    }

    public AccountRoleId getId() {
        return id;
    }

    public UUID getAccountId() {
        return id.getAccountId();
    }

    public UUID getRoleId() {
        return id.getRoleId();
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public long getVersion() {
        return version;
    }
}
