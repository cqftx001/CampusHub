package com.campushub.auth.service.impl;

import com.campushub.auth.config.PasswordResetProperties;
import com.campushub.auth.domain.*;
import com.campushub.auth.dto.PasswordChangeRequest;
import com.campushub.auth.dto.PasswordResetRequest;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.repository.AuthAccountRepository;
import com.campushub.auth.repository.LoginSessionRepository;
import com.campushub.auth.repository.PasswordCredentialRepository;
import com.campushub.auth.repository.RefreshTokenRepository;
import com.campushub.auth.service.PasswordService;
import com.campushub.auth.token.AccessTokenRegistry;
import com.campushub.auth.utils.AuthInputNormalizer;
import com.campushub.auth.utils.SecureTokenGenerator;
import com.campushub.auth.utils.Sha256Hasher;
import com.campushub.auth.dto.PasswordResetConfirmRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PasswordServiceImpl implements PasswordService {

    private final PasswordCredentialRepository passwordCredentialRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAccountRepository authAccountRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender javaMailSender;
    private final PasswordResetProperties properties;
    private final SecureTokenGenerator tokenGenerator;
    private final Sha256Hasher sha256Hasher;
    private final AuthInputNormalizer inputNormalizer;

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordServiceImpl.class);

    private final PasswordEncoder passwordEncoder;
    private final AccessTokenRegistry accessTokenRegistry;
    private final Clock clock;

    private static final String RESET_TOKEN_KEY_PREFIX = "auth:password:reset:token:";
    private static final String RESET_ACCOUNT_KEY_PREFIX = "auth:password:reset:account:";
    private static final String RESET_REQUEST_KEY_PREFIX = "auth:password:reset:request:";

    private static final int MAX_RESET_TOKEN_LENGTH = 128;

    public PasswordServiceImpl(
            PasswordCredentialRepository passwordCredentialRepository,
            LoginSessionRepository loginSessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenRegistry accessTokenRegistry,
            AuthAccountRepository authAccountRepository,
            StringRedisTemplate stringRedisTemplate,
            JavaMailSender javaMailSender,
            PasswordResetProperties passwordResetProperties,
            SecureTokenGenerator tokenGenerator,
            Sha256Hasher sha256Hasher,
            AuthInputNormalizer inputNormalizer,
            Clock clock
    ) {
        this.passwordCredentialRepository =
                passwordCredentialRepository;
        this.loginSessionRepository =
                loginSessionRepository;
        this.refreshTokenRepository =
                refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenRegistry = accessTokenRegistry;
        this.authAccountRepository = authAccountRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.javaMailSender = javaMailSender;
        this.properties = passwordResetProperties;
        this.tokenGenerator = tokenGenerator;
        this.sha256Hasher = sha256Hasher;
        this.inputNormalizer = inputNormalizer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void changePassword(UUID accountId, PasswordChangeRequest request) {
        PasswordCredential credential = passwordCredentialRepository
                .findByIdForUpdate(accountId)
                .orElseThrow(this::currentPasswordInvalid);

        if (!passwordEncoder.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw currentPasswordInvalid();
        }

        if (passwordEncoder.matches(request.newPassword(), credential.getPasswordHash())) {
            throw new AuthException(
                    AuthErrorCode.NEW_PASSWORD_MUST_DIFFER
            );
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        List<LoginSession> activeSessions = loginSessionRepository
                .findAllByAccountIdAndStatusForUpdate(accountId, LoginSessionStatus.ACTIVE);

        List<RefreshToken> activeRefreshTokens = lockActiveRefreshTokens(activeSessions);

        accessTokenRegistry.revokeAll(
                accountId,
                activeSessions.stream().map(LoginSession::getId).toList()
        );

        Instant changedAt = clock.instant();

        activeRefreshTokens.forEach(token -> token.revoke(changedAt));

        activeSessions.forEach(token -> token.revoke(changedAt));

        credential.changePassword(newPasswordHash, changedAt);
    }

    @Override
    public void requestPasswordReset(PasswordResetRequest request) {
        String normalizedEmail = inputNormalizer.normalizeCaseInsensitive(request.email());

        if(normalizedEmail == null) return;

        String cooldownLease = UUID.randomUUID().toString();
        String cooldownKey = RESET_REQUEST_KEY_PREFIX + sha256Hasher.hash(normalizedEmail);

        if(!tryAcquireResetCooldown(cooldownKey, cooldownLease)) {
            return;
        }

        Optional<AuthAccount> account = authAccountRepository.findByEmail(normalizedEmail);


        /*
         * 未知邮箱、未验证邮箱、没有本地密码的账户
         * 全部静默返回，由 Controller 统一响应 202。
         */
        if (account.isEmpty() || account.get().getEmailVerifiedAt() == null
                || !passwordCredentialRepository.existsById(account.get().getId())) {
            return;
        }

        IssuedPasswordResetToken issuedToken;

        try {
            issuedToken = replaceCurrentResetToken(account.get().getId());
        } catch (AuthException e) {
            if(e.getErrorCode() != AuthErrorCode.PASSWORD_RESET_UNAVAILABLE) {
                throw e;
            }

            tryReleaseResetCooldown(
                    cooldownKey,
                    cooldownLease
            );

            LOGGER.warn("Unable to create a password-reset token", e);

            return;
        }

        try {
            sendPasswordResetEmail(
                    normalizedEmail,
                    issuedToken.rawToken()
            );
        } catch (MailException e) {
            tryCleanupResetToken(issuedToken);

            tryReleaseResetCooldown(
                    cooldownKey,
                    cooldownLease
            );

            LOGGER.warn("Unable to send a password-reset email", e);
        }
    }

    @Override
    @Transactional
    public void confirmPasswordReset(
            PasswordResetConfirmRequest request
    ) {
        PasswordResetTokenClaim tokenClaim =
                findResetTokenClaim(request.token())
                        .orElseThrow(
                                this::invalidPasswordResetToken
                        );

        /*
         * 与普通 login 使用同一个 credential lock。
         * 旧密码登录无法在密码重置过程中创建漏网 Session。
         */
        PasswordCredential credential =
                passwordCredentialRepository
                        .findByIdForUpdate(
                                tokenClaim.accountId()
                        )
                        .orElseThrow(
                                this::invalidPasswordResetToken
                        );

        if (passwordEncoder.matches(
                request.newPassword(),
                credential.getPasswordHash()
        )) {
            throw new AuthException(
                    AuthErrorCode.NEW_PASSWORD_MUST_DIFFER
            );
        }

        String newPasswordHash =
                passwordEncoder.encode(
                        request.newPassword()
                );

        List<LoginSession> activeSessions =
                loginSessionRepository
                        .findAllByAccountIdAndStatusForUpdate(
                                tokenClaim.accountId(),
                                LoginSessionStatus.ACTIVE
                        );

        List<RefreshToken> activeRefreshTokens =
                lockActiveRefreshTokens(activeSessions);

        /*
         * 上面的 credential/session locks 已经冻结了认证状态。
         * 此时才原子消费 Reset Token。
         */
        if (!consumeResetToken(tokenClaim)) {
            throw invalidPasswordResetToken();
        }

        /*
         * Redis Session 撤销失败时数据库事务回滚，
         * 但 Reset Token 已经被安全消费。
         * 用户需要重新请求一个 Token。
         */
        accessTokenRegistry.revokeAll(
                tokenClaim.accountId(),
                activeSessions.stream()
                        .map(LoginSession::getId)
                        .toList()
        );

        Instant changedAt = clock.instant();

        activeRefreshTokens.forEach(
                token -> token.revoke(changedAt)
        );

        activeSessions.forEach(
                session -> session.revoke(changedAt)
        );

        credential.changePassword(
                newPasswordHash,
                changedAt
        );
    }

    // --- helper ---
    private boolean tryAcquireResetCooldown(
            String cooldownKey,
            String cooldownLease
    ) {
        try {
            Boolean acquired =
                    stringRedisTemplate.opsForValue()
                            .setIfAbsent(
                                    cooldownKey,
                                    cooldownLease,
                                    properties
                                            .requestCooldown()
                            );

            if (acquired == null) {
                throw passwordResetUnavailable();
            }

            return acquired;
        } catch (DataAccessException exception) {
            throw passwordResetUnavailable();
        }
    }

    private IssuedPasswordResetToken
    replaceCurrentResetToken(UUID accountId) {
        String rawToken = tokenGenerator.generate();

        String tokenDigest =
                sha256Hasher.hash(rawToken);

        String accountKey =
                RESET_ACCOUNT_KEY_PREFIX + accountId;

        String tokenKey =
                RESET_TOKEN_KEY_PREFIX
                        + tokenDigest;

        try {
            Long result = stringRedisTemplate.execute(
                    REPLACE_RESET_TOKEN_SCRIPT,
                    List.of(accountKey, tokenKey),
                    RESET_TOKEN_KEY_PREFIX,
                    accountId.toString(),
                    tokenDigest,
                    Long.toString(
                            properties.tokenTtl()
                                    .toMillis()
                    )
            );

            if (!Long.valueOf(1L).equals(result)) {
                throw passwordResetUnavailable();
            }
        } catch (DataAccessException exception) {
            throw passwordResetUnavailable();
        }

        return new IssuedPasswordResetToken(
                accountId,
                rawToken,
                tokenDigest
        );
    }

    private void tryCleanupResetToken(
            IssuedPasswordResetToken issuedToken
    ) {
        try {
            stringRedisTemplate.execute(
                    REMOVE_RESET_TOKEN_SCRIPT,
                    List.of(
                            RESET_ACCOUNT_KEY_PREFIX
                                    + issuedToken.accountId(),
                            RESET_TOKEN_KEY_PREFIX
                                    + issuedToken.tokenDigest()
                    ),
                    issuedToken.tokenDigest(),
                    issuedToken.accountId().toString()
            );
        } catch (DataAccessException exception) {
            LOGGER.warn(
                    "Unable to clean up a password-reset token",
                    exception
            );
        }
    }

    private void tryReleaseResetCooldown(
            String cooldownKey,
            String cooldownLease
    ) {
        try {
            stringRedisTemplate.execute(
                    RELEASE_RESET_COOLDOWN_SCRIPT,
                    List.of(cooldownKey),
                    cooldownLease
            );
        } catch (DataAccessException exception) {
            LOGGER.warn(
                    "Unable to release a password-reset cooldown",
                    exception
            );
        }
    }

    private void sendPasswordResetEmail(String email, String rawToken) {
        String resetLink = UriComponentsBuilder
                .fromUriString(properties.resetUrl())
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.fromAddress());
        message.setTo(email);
        message.setSubject("Reset your CampusHub password");
        message.setText("Reset your CampusHub password using this link: "+ resetLink);
        javaMailSender.send(message);
    }

    private List<RefreshToken> lockActiveRefreshTokens(List<LoginSession> activeSessions) {
        List<RefreshToken> activeRefreshTokens = new ArrayList<>();

        for (LoginSession session : activeSessions) {
            refreshTokenRepository
                    .findBySessionIdAndStatusForUpdate(
                            session.getId(),
                            RefreshTokenStatus.ACTIVE
                    )
                    .ifPresent(activeRefreshTokens::add);
        }
        return activeRefreshTokens;
    }
    private AuthException currentPasswordInvalid() {
        return new AuthException(AuthErrorCode.CURRENT_PASSWORD_INVALID);
    }

    private AuthException passwordResetUnavailable() {
        return new AuthException(
                AuthErrorCode.PASSWORD_RESET_UNAVAILABLE
        );
    }

    private record IssuedPasswordResetToken(
            UUID accountId,
            String rawToken,
            String tokenDigest
    ) {
    }

    private Optional<PasswordResetTokenClaim>
    findResetTokenClaim(String rawToken) {
        String normalizedToken =
                normalizeResetToken(rawToken);

        if (normalizedToken == null) {
            return Optional.empty();
        }

        String tokenDigest =
                sha256Hasher.hash(normalizedToken);

        String tokenKey =
                RESET_TOKEN_KEY_PREFIX
                        + tokenDigest;

        try {
            String accountValue =
                    stringRedisTemplate.opsForValue()
                            .get(tokenKey);

            if (accountValue == null) {
                return Optional.empty();
            }

            UUID accountId;

            try {
                accountId =
                        UUID.fromString(accountValue);
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }

            String accountKey =
                    RESET_ACCOUNT_KEY_PREFIX
                            + accountId;

            Long valid = stringRedisTemplate.execute(
                    VALIDATE_RESET_TOKEN_SCRIPT,
                    List.of(tokenKey, accountKey),
                    accountValue,
                    tokenDigest
            );

            if (!Long.valueOf(1L).equals(valid)) {
                return Optional.empty();
            }

            return Optional.of(
                    new PasswordResetTokenClaim(
                            accountId,
                            tokenDigest
                    )
            );
        } catch (DataAccessException exception) {
            throw passwordResetUnavailable();
        }
    }

    private boolean consumeResetToken(
            PasswordResetTokenClaim tokenClaim
    ) {
        String tokenKey =
                RESET_TOKEN_KEY_PREFIX
                        + tokenClaim.tokenDigest();

        String accountKey =
                RESET_ACCOUNT_KEY_PREFIX
                        + tokenClaim.accountId();

        try {
            Long consumed =
                    stringRedisTemplate.execute(
                            CONSUME_RESET_TOKEN_SCRIPT,
                            List.of(
                                    tokenKey,
                                    accountKey
                            ),
                            tokenClaim.accountId()
                                    .toString(),
                            tokenClaim.tokenDigest()
                    );

            return Long.valueOf(1L)
                    .equals(consumed);
        } catch (DataAccessException exception) {
            throw passwordResetUnavailable();
        }
    }

    private String normalizeResetToken(
            String token
    ) {
        if (token == null) {
            return null;
        }

        String normalized = token.strip();

        if (normalized.isEmpty()
                || normalized.length()
                > MAX_RESET_TOKEN_LENGTH) {
            return null;
        }

        /*
         * Token 大小写敏感，绝对不能调用
         * normalizeCaseInsensitive。
         */
        return normalized;
    }

    private AuthException invalidPasswordResetToken() {
        return new AuthException(
                AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID
        );
    }

    private record PasswordResetTokenClaim(
            UUID accountId,
            String tokenDigest
    ) {
    }

    private static final DefaultRedisScript<Long>
            REPLACE_RESET_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                local previousDigest =
                    redis.call('GET', KEYS[1])

                if previousDigest then
                    redis.call(
                        'DEL',
                        ARGV[1] .. previousDigest
                    )
                end

                redis.call(
                    'SET',
                    KEYS[2],
                    ARGV[2],
                    'PX',
                    ARGV[4]
                )

                redis.call(
                    'SET',
                    KEYS[1],
                    ARGV[3],
                    'PX',
                    ARGV[4]
                )

                return 1
                """, Long.class);

    private static final DefaultRedisScript<Long>
            REMOVE_RESET_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                if redis.call('GET', KEYS[1])
                    == ARGV[1] then
                    redis.call('DEL', KEYS[1])
                end

                if redis.call('GET', KEYS[2])
                    == ARGV[2] then
                    redis.call('DEL', KEYS[2])
                end

                return 1
                """, Long.class);

    private static final DefaultRedisScript<Long>
            RELEASE_RESET_COOLDOWN_SCRIPT =
            new DefaultRedisScript<>("""
                if redis.call('GET', KEYS[1])
                    == ARGV[1] then
                    return redis.call('DEL', KEYS[1])
                end

                return 0
                """, Long.class);

    private static final DefaultRedisScript<Long>
            VALIDATE_RESET_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                local tokenAccount =
                    redis.call('GET', KEYS[1])

                local currentDigest =
                    redis.call('GET', KEYS[2])

                if tokenAccount == ARGV[1]
                    and currentDigest == ARGV[2] then
                    return 1
                end

                return 0
                """, Long.class);

    private static final DefaultRedisScript<Long>
            CONSUME_RESET_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                local tokenAccount =
                    redis.call('GET', KEYS[1])

                local currentDigest =
                    redis.call('GET', KEYS[2])

                if tokenAccount ~= ARGV[1]
                    or currentDigest ~= ARGV[2] then
                    return 0
                end

                redis.call(
                    'DEL',
                    KEYS[1],
                    KEYS[2]
                )

                return 1
                """, Long.class);

}
