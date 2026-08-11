package com.campushub.user.domain;

import com.campushub.shared.base.BaseEntity;
import com.campushub.user.enums.Gender;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "profiles",
        schema = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_profiles_account_id",
                        columnNames = "account_id"
                )
        }
)
public class UserProfile extends BaseEntity {

    /**
     * 1. accountId
     * 2. avatarUrl
     * 3. gender
     * 4. birthdate
     * 5. firstName
     * 6. lastName
     */
    @Column(
            name = "account_id",
            nullable = false,
            updatable = false
    )
    private UUID accountId;

    @Column(
            name = "avatar_url",
            length = 2048
    )
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "gender",
            length = 32
    )
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    protected UserProfile() {
    }

    public UserProfile(UUID accountId) {
        this.accountId = accountId;
    }

    public void replaceProfile(
            String avatarUrl,
            Gender gender,
            LocalDate birthDate,
            String firstName,
            String lastName
    ) {
        this.avatarUrl = avatarUrl;
        this.gender = gender;
        this.birthDate = birthDate;
        this.firstName = normalizeName(firstName);
        this.lastName = normalizeName(lastName);
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
