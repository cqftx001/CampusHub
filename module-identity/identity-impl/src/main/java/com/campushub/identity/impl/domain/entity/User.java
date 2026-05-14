package com.campushub.identity.impl.domain.entity;

import com.campushub.identity.impl.domain.enums.OAuthProvider;
import com.campushub.identity.impl.domain.enums.UserStatus;
import com.campushub.infrastructure.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户认证实体 —— 只包含登录和身份验证必需的字段。
 *
 * <h2>为什么这么瘦？</h2>
 * 登录是系统最高频的操作。如果 users 表有 20+ 字段，
 * 每次登录 SELECT * 都会拉回一堆 bio、school_id、address 等无关数据。
 *
 * 拆分后 users 表只有 12 个字段（不到 1KB/行），
 * 能被 PostgreSQL 的 shared_buffers 完全缓存，登录查询极快。
 *
 * 个人资料（姓名、学校、地址等）放在 UserProfile 实体里，
 * 只在用户查看/编辑个人主页时才查询。
 *
 * @author Kevin
 */
@Getter
@Entity
@Table(name = "users", schema = "identity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 30)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    // ==================== OAuth ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id", length = 255)
    private String oauthId;

    // ==================== 构造方法 ====================

    private User(
            String email,
            String username,
            String passwordHash,
            String displayName,
            UserStatus status,
            OAuthProvider oauthProvider,
            String oauthId
    ) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }
    // ==================== Static Factory ====================
    public static User registerLocal(
            String email,
            String username,
            String passwordHash,
            String displayName
    ) {
        return new User(
                email,
                username,
                passwordHash,
                displayName,
                UserStatus.UNVERIFIED,
                null,
                null
        );
    }

    public static User registerOAuth(
            String email,
            String username,
            String displayName,
            OAuthProvider oauthProvider,
            String oauthId
    ) {
        return new User(
                email,
                username,
                null,
                displayName,
                UserStatus.ACTIVE,
                oauthProvider,
                oauthId
        );
    }
    // ==================== 业务方法 ====================

    public void activate() {
        if (this.status != UserStatus.UNVERIFIED) {
            throw new IllegalStateException("Only UNVERIFIED users can be activated");
        }
        this.status = UserStatus.ACTIVE;
    }

    public void suspend() {
        if (this.status == UserStatus.DELETED) {
            throw new IllegalStateException("Cannot suspend a deleted user");
        }
        this.status = UserStatus.SUSPENDED;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean hasPassword() {
        return this.passwordHash != null && !this.passwordHash.isBlank();
    }

    public boolean hasOAuthBinding() {
        return this.oauthProvider != null && this.oauthId != null && !this.oauthId.isBlank();
    }

    public boolean isOAuthOnlyUser() {
        return hasOAuthBinding() && !hasPassword();
    }

    public boolean canLoginWithPassword() {
        return hasPassword();
    }
}