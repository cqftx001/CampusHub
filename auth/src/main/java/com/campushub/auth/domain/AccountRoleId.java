package com.campushub.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AccountRoleId implements Serializable {

    @Column(
            name = "account_id",
            nullable = false,
            updatable = false
    )
    private UUID accountId;

    @Column(
            name = "role_id",
            nullable = false,
            updatable = false
    )
    private UUID roleId;

    protected AccountRoleId() {
    }

    public AccountRoleId(UUID accountId, UUID roleId) {
        this.accountId = Objects.requireNonNull(accountId);
        this.roleId = Objects.requireNonNull(roleId);
    }

    public UUID getAccountId(){
        return accountId;
    }

    public UUID getRoleId(){
        return roleId;
    }

    @Override
    public boolean equals(Object object){
        if(this == object) return true;

        if(!(object instanceof AccountRoleId other)) return false;

        return accountId.equals(other.accountId) && roleId.equals(other.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, roleId);
    }
}
