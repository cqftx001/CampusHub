package com.campushub.auth.service.impl;

import com.campushub.auth.config.EmailVerificationProperties;
import com.campushub.auth.domain.AuthAccount;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.repository.AuthAccountRepository;
import com.campushub.auth.service.EmailVerificationService;
import com.campushub.auth.utils.AuthInputNormalizer;
import com.campushub.auth.utils.SecureTokenGenerator;
import com.campushub.auth.utils.Sha256Hasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailVerificationServiceImpl.class);

    private static final String TOKEN_KEY_PREFIX =
            "auth:email:verification:token:";
    private static final String ACCOUNT_KEY_PREFIX =
            "auth:email:verification:account:";
    private static final String RESEND_KEY_PREFIX =
            "auth:email:verification:resend:";

    private static final int MAX_TOKEN_LENGTH = 128;

    private static final DefaultRedisScript<Long> REPLACE_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                    local previous = redis.call('GET', KEYS[1])
                    if previous then
                        redis.call('DEL', ARGV[1] .. previous)
                    end
                    redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[4])
                    redis.call('SET', KEYS[1], ARGV[3], 'PX', ARGV[4])
                    return 1
                    """, Long.class);

    private static final DefaultRedisScript<Long> VALIDATE_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                    local tokenAccount = redis.call('GET', KEYS[1])
                    local currentToken = redis.call('GET', KEYS[2])
                    if tokenAccount == ARGV[1] and currentToken == ARGV[2] then
                        return 1
                    end
                    return 0
                    """, Long.class);

    private static final DefaultRedisScript<Long> REMOVE_TOKEN_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        redis.call('DEL', KEYS[1])
                    end
                    if redis.call('GET', KEYS[2]) == ARGV[2] then
                        redis.call('DEL', KEYS[2])
                    end
                    return 1
                    """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_COOLDOWN_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """, Long.class);

    private final AuthAccountRepository authAccountRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender javaMailSender;
    private final EmailVerificationProperties emailVerificationProperties;
    private final SecureTokenGenerator tokenGenerator;
    private final Sha256Hasher sha256Hasher;
    private final AuthInputNormalizer inputNormalizer;

    public EmailVerificationServiceImpl(
            AuthAccountRepository authAccountRepository,
            StringRedisTemplate stringRedisTemplate,
            JavaMailSender javaMailSender,
            EmailVerificationProperties emailVerificationProperties,
            SecureTokenGenerator tokenGenerator,
            Sha256Hasher sha256Hasher,
            AuthInputNormalizer inputNormalizer
    ) {
        this.authAccountRepository =
                authAccountRepository;
        this.stringRedisTemplate =
                stringRedisTemplate;
        this.javaMailSender =
                javaMailSender;
        this.emailVerificationProperties =
                emailVerificationProperties;
        this.tokenGenerator = tokenGenerator;
        this.sha256Hasher = sha256Hasher;
        this.inputNormalizer = inputNormalizer;
    }

    @Override
    public void sendInitialVerification(UUID accountId, String email) {
        String normalizedEmail = inputNormalizer.normalizeCaseInsensitive(email);
        if (accountId == null || normalizedEmail == null) {
            return;
        }

        Optional<AuthAccount> account = authAccountRepository.findById(accountId);
        if (account.isEmpty()
                || account.get().getEmailVerifiedAt() != null
                || !normalizedEmail.equals(inputNormalizer.normalizeCaseInsensitive(account.get().getEmail()))) {
            return;
        }

        IssuedToken issuedToken = replaceCurrentToken(accountId);
        try {
            sendVerificationEmail(normalizedEmail, issuedToken.rawToken());
        } catch (MailException exception) {
            tryCleanupToken(issuedToken);
            throw verificationUnavailable();
        }
    }

    @Override
    @Transactional
    public void confirm(String token) {
        UUID accountId = findCurrentAccount(token)
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID
                ));

        AuthAccount account = authAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID
                ));

        account.verifyEmail(Instant.now());
    }

    @Override
    public void resend(String email) {
        String normalizedEmail = inputNormalizer.normalizeCaseInsensitive(email);
        if (normalizedEmail == null) {
            return;
        }

        String cooldownLease = UUID.randomUUID().toString();
        String cooldownKey = RESEND_KEY_PREFIX + sha256Hasher.hash(normalizedEmail);
        if (!tryAcquireCooldown(cooldownKey, cooldownLease)) {
            return;
        }

        Optional<AuthAccount> account = authAccountRepository.findByEmail(normalizedEmail);
        if (account.isEmpty() || account.get().getEmailVerifiedAt() != null) {
            return;
        }

        IssuedToken issuedToken;
        try {
            issuedToken = replaceCurrentToken(account.get().getId());
        } catch (AuthException exception) {
            tryReleaseCooldown(cooldownKey, cooldownLease);
            throw exception;
        }

        try {
            sendVerificationEmail(normalizedEmail, issuedToken.rawToken());
        } catch (MailException exception) {
            tryCleanupToken(issuedToken);
            tryReleaseCooldown(cooldownKey, cooldownLease);
            LOGGER.warn("Unable to resend an email verification message", exception);
        }
    }

    // --- helper ---

    private IssuedToken replaceCurrentToken(UUID accountId) {
        String rawToken = tokenGenerator.generate();
        String tokenDigest = sha256Hasher.hash(rawToken);
        String accountKey = ACCOUNT_KEY_PREFIX + accountId;
        String tokenKey = TOKEN_KEY_PREFIX + tokenDigest;

        try {
            Long result = stringRedisTemplate.execute(
                    REPLACE_TOKEN_SCRIPT,
                    List.of(accountKey, tokenKey),
                    TOKEN_KEY_PREFIX,
                    accountId.toString(),
                    tokenDigest,
                    Long.toString(emailVerificationProperties.tokenTtl().toMillis())
            );
            if (!Long.valueOf(1L).equals(result)) {
                throw verificationUnavailable();
            }
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }

        return new IssuedToken(accountId, rawToken, tokenDigest);
    }

    private Optional<UUID> findCurrentAccount(String token) {
        String normalizedToken = normalizeToken(token);
        if (normalizedToken == null) {
            return Optional.empty();
        }

        String tokenDigest = sha256Hasher.hash(normalizedToken);
        String tokenKey = TOKEN_KEY_PREFIX + tokenDigest;

        try {
            String accountValue = stringRedisTemplate.opsForValue().get(tokenKey);
            if (accountValue == null) {
                return Optional.empty();
            }

            UUID accountId;
            try {
                accountId = UUID.fromString(accountValue);
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }

            String accountKey = ACCOUNT_KEY_PREFIX + accountId;
            Long valid = stringRedisTemplate.execute(
                    VALIDATE_TOKEN_SCRIPT,
                    List.of(tokenKey, accountKey),
                    accountValue,
                    tokenDigest
            );

            return Long.valueOf(1L).equals(valid)
                    ? Optional.of(accountId)
                    : Optional.empty();
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return null;
        }

        String normalized = token.strip();

        if (normalized.isEmpty()
                || normalized.length()
                > MAX_TOKEN_LENGTH) {
            return null;
        }

        return normalized;
    }

    private boolean tryAcquireCooldown(String cooldownKey, String cooldownLease) {
        try {
            return Boolean.TRUE.equals(
                    stringRedisTemplate.opsForValue().setIfAbsent(
                            cooldownKey,
                            cooldownLease,
                            emailVerificationProperties.resendCooldown()
                    )
            );
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }
    }

    private void tryCleanupToken(IssuedToken issuedToken) {
        try {
            stringRedisTemplate.execute(
                    REMOVE_TOKEN_SCRIPT,
                    List.of(
                            ACCOUNT_KEY_PREFIX + issuedToken.accountId(),
                            TOKEN_KEY_PREFIX + issuedToken.tokenDigest()
                    ),
                    issuedToken.tokenDigest(),
                    issuedToken.accountId().toString()
            );
        } catch (DataAccessException exception) {
            LOGGER.warn("Unable to clean up an email verification token", exception);
        }
    }

    private void tryReleaseCooldown(String cooldownKey, String cooldownLease) {
        try {
            stringRedisTemplate.execute(
                    RELEASE_COOLDOWN_SCRIPT,
                    List.of(cooldownKey),
                    cooldownLease
            );
        } catch (DataAccessException exception) {
            LOGGER.warn("Unable to release an email verification cooldown", exception);
        }
    }

    private void sendVerificationEmail(String email, String token) {
        String verificationLink = UriComponentsBuilder
                .fromUriString(emailVerificationProperties.verificationUrl())
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailVerificationProperties.fromAddress());
        message.setTo(email);
        message.setSubject("Verify your CampusHub email address");
        message.setText("Verify your email address using this link: " + verificationLink);
        javaMailSender.send(message);
    }

    private AuthException verificationUnavailable() {
        return new AuthException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE);
    }

    private record IssuedToken(
            UUID accountId,
            String rawToken,
            String tokenDigest
    ) {
    }
}
