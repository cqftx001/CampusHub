package com.campushub.auth.service.impl;

import com.campushub.auth.config.EmailVerificationProperties;
import com.campushub.auth.domain.AuthAccount;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.repository.AuthAccountRepository;
import com.campushub.auth.utils.AuthInputNormalizer;
import com.campushub.auth.utils.SecureTokenGenerator;
import com.campushub.auth.utils.Sha256Hasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JavaMailSender javaMailSender;

    private EmailVerificationServiceImpl service;


    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new EmailVerificationServiceImpl(
            authAccountRepository,
            stringRedisTemplate,
            javaMailSender,
            new EmailVerificationProperties(TOKEN_TTL, RESEND_COOLDOWN,
                "https://campushub.test/verify-email",
                    "no-reply@campushub.test"),
        new SecureTokenGenerator(),
        new Sha256Hasher(),
        new AuthInputNormalizer());
    }

    @Test
    void confirmEnablesAccountAndIsIdempotent() {
        UUID accountId = UUID.randomUUID();
        AuthAccount account = new AuthAccount(
                "campus.user",
                "campus.user@example.com"
        );
        stubCurrentToken(accountId);
        when(authAccountRepository.findByIdForUpdate(accountId))
                .thenReturn(Optional.of(account));

        service.confirm("current-token");
        Instant firstVerifiedAt = account.getEmailVerifiedAt();
        service.confirm("current-token");

        assertThat(account.isEnabled()).isTrue();
        assertThat(account.getEmailVerifiedAt()).isEqualTo(firstVerifiedAt);
    }

    @Test
    void confirmRejectsUnknownToken() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.confirm("unknown-token"))
                .isInstanceOfSatisfying(AuthException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AuthErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID)
                );
    }

    @Test
    void resendReturnsSilentlyForUnknownEmail() {
        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                eq(RESEND_COOLDOWN)
        )).thenReturn(true);
        when(authAccountRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        service.resend(" Missing@Example.com ");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void resendReturnsSilentlyForVerifiedEmail() {
        AuthAccount account = new AuthAccount(
                "verified.user",
                "verified@example.com"
        );
        account.verifyEmail(Instant.parse("2026-08-23T12:00:00Z"));
        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                eq(RESEND_COOLDOWN)
        )).thenReturn(true);
        when(authAccountRepository.findByEmail("verified@example.com"))
                .thenReturn(Optional.of(account));

        service.resend("verified@example.com");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void resendReturnsSilentlyDuringCooldown() {
        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                eq(RESEND_COOLDOWN)
        )).thenReturn(false);

        service.resend("campus.user@example.com");

        verify(authAccountRepository, never()).findByEmail(anyString());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void initialMailFailureDoesNotLeaveCurrentToken() {
        UUID accountId = UUID.randomUUID();
        AuthAccount account = new AuthAccount(
                "campus.user",
                "campus.user@example.com"
        );
        when(authAccountRepository.findById(accountId))
                .thenReturn(Optional.of(account));
        stubSuccessfulScriptExecution();
        doThrow(new MailSendException("SMTP unavailable"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.sendInitialVerification(
                accountId,
                "campus.user@example.com"
        )).isInstanceOfSatisfying(AuthException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE)
        );

        verify(stringRedisTemplate, atLeast(2)).execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        );
    }

    private void stubCurrentToken(UUID accountId) {
        when(valueOperations.get(anyString())).thenReturn(accountId.toString());
        stubSuccessfulScriptExecution();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubSuccessfulScriptExecution() {
        doReturn(1L).when(stringRedisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        );
    }
}
