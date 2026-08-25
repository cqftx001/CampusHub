package com.campushub.auth.domain;

import com.campushub.shared.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "accounts",
        schema = "auth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_auth_accounts_username",
                        columnNames = "username"
                ),
                @UniqueConstraint(
                        name = "uk_auth_accounts_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uk_auth_accounts_phone_number",
                        columnNames = "phone_number"
                )
        }
)
public class AuthAccount extends BaseEntity {

    @Column(nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", length = 16)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    protected AuthAccount() {
    }

    public AuthAccount(
            String username,
            String email
    ) {
        this.username = Objects.requireNonNull(username);
        this.email = email;
        this.enabled = false;
    }

    public void verifyEmail(Instant verifiedAt){
        if(email == null){
            throw new IllegalStateException("Account does not have an email address.");
        }

        if (emailVerifiedAt != null) {
            return;
        }

        this.emailVerifiedAt = Objects.requireNonNull(verifiedAt);

        this.enabled = true;
    }

    public void activate() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

}
