package com.campushub.identity.impl.domain.enums;

/**
 *  * 状态流转：
 *  *   注册 → UNVERIFIED
 *  *   邮箱验证通过 → ACTIVE
 *  *   管理员封禁 → SUSPENDED
 *  *   用户注销 → DELETED（软删除，数据保留 30 天）
 *  *
 */
public enum UserStatus {
    UNVERIFIED,
    ACTIVE,
    SUSPENDED,
    DELETED
}
