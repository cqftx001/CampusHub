package com.campushub.identity.impl.domain.entity;

import com.campushub.infrastructure.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 用户个人资料实体 —— 低频访问，只在查看/编辑个人主页时查询。
 *
 * 和 User 是 1:1 关系，通过 userId 关联（不用 JPA @OneToOne，避免 N+1 查询）。
 *
 * <h2>为什么不用 @OneToOne 关联 User？</h2>
 * JPA 的 @OneToOne 有个坑：即使你只查 User，
 * Hibernate 默认会额外发一条 SELECT 查 UserProfile（因为它要判断关联是否为 null）。
 * 这叫 N+1 问题。用 userId 纯字段关联，查 User 时不会触发 profile 的查询。
 *
 * @author Kevin
 */
@Getter
@Entity
@Table(name = "user_profiles", schema = "identity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Column(name = "school_id", length = 50)
    private String schoolId;

    @Column(length = 100)
    private String major;

    @Column(name = "graduation_year", length = 4)
    private String graduationYear;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(length = 100)
    private String country;

    @Column(name = "preferred_contact_method", length = 20)
    private String preferredContactMethod;

    // Private Constructor
    private UserProfile(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        this.userId = userId;
    }

    // Factory Method
    public static UserProfile createForUser(UUID userId) {
        return new UserProfile(userId);
    }

    public void update(ProfileData data) {
        if (data == null) {
            throw new IllegalArgumentException("Profile data must not be null");
        }

        this.firstName = data.firstName();
        this.lastName = data.lastName();
        this.phone = data.phone();
        this.avatarUrl = data.avatarUrl();
        this.bio = data.bio();
        this.schoolId = data.schoolId();
        this.major = data.major();
        this.graduationYear = data.graduationYear();
        this.preferredContactMethod = data.preferredContactMethod();

        Address address = data.address();
        if (address != null) {
            this.addressLine1 = address.addressLine1();
            this.addressLine2 = address.addressLine2();
            this.city = address.city();
            this.state = address.state();
            this.zipCode = address.zipCode();
            this.country = address.country();
        }
    }
}