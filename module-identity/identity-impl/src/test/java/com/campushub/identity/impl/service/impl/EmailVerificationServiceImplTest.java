package com.campushub.identity.impl.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushub.identity.impl.config.EmailVerificationProperties;
import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.shared.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock JavaMailSender mailSender;
    @Mock EmailVerificationServiceImpl self;

    EmailVerificationServiceImpl service;

    static final EmailVerificationProperties PROPS = new EmailVerificationProperties(
            6, Duration.ofMinutes(5), Duration.ofSeconds(60), 3, "test-pepper"
    );

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new EmailVerificationServiceImpl(redisTemplate, mailSender, PROPS, self);
    }

    // ──────────────── sendRegisterCode ────────────────

    @Nested
    class SendRegisterCode {

        @Test
        void storesHashAndDelegatesToAsyncSend() {
            when(valueOps.setIfAbsent(anyString(), eq("1"), eq(PROPS.resendInterval())))
                    .thenReturn(true);

            service.sendRegisterCode("User@Example.COM");

            verify(valueOps).setIfAbsent(
                    eq("email:register:limit:user@example.com"),
                    eq("1"), eq(PROPS.resendInterval()));
            verify(valueOps).set(
                    eq("email:register:code:user@example.com"),
                    anyString(), eq(PROPS.codeTtl()));
            verify(redisTemplate).delete("email:register:attempts:user@example.com");
            verify(self).sendEmailAsync(eq("user@example.com"), anyString(), any());
        }

        @Test
        void generatedCodeHasConfiguredLength() {
            when(valueOps.setIfAbsent(anyString(), eq("1"), eq(PROPS.resendInterval())))
                    .thenReturn(true);

            service.sendRegisterCode("user@example.com");

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(self).sendEmailAsync(anyString(), codeCaptor.capture(), any());

            String code = codeCaptor.getValue();
            assertThat(code).hasSize(PROPS.codeLength());
            assertThat(code).matches("\\d+");
        }

        @Test
        void rejectsWhenResendIntervalNotElapsed() {
            when(valueOps.setIfAbsent(anyString(), eq("1"), eq(PROPS.resendInterval())))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.sendRegisterCode("user@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .satisfies(e -> assertThat(((BadRequestException) e).getErrorCode())
                            .isEqualTo(IdentityErrorCode.EMAIL_CODE_RESEND_INTERVAL));

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
            verify(self, never()).sendEmailAsync(anyString(), anyString(), any());
        }

        @Test
        void normalizesEmailToLowerCaseAndTrims() {
            when(valueOps.setIfAbsent(anyString(), eq("1"), eq(PROPS.resendInterval())))
                    .thenReturn(true);

            service.sendRegisterCode("  User@EXAMPLE.COM  ");

            verify(self).sendEmailAsync(eq("user@example.com"), anyString(), any());
        }
    }

    // ──────────────── sendLoginCode ────────────────

    @Nested
    class SendLoginCode {

        @Test
        void usesLoginKeyPrefix() {
            when(valueOps.setIfAbsent(anyString(), eq("1"), eq(PROPS.resendInterval())))
                    .thenReturn(true);

            service.sendLoginCode("user@example.com");

            verify(valueOps).setIfAbsent(
                    eq("email:login:limit:user@example.com"),
                    eq("1"), eq(PROPS.resendInterval()));
            verify(valueOps).set(
                    startsWith("email:login:code:"),
                    anyString(), eq(PROPS.codeTtl()));
        }
    }

    // ──────────────── verifyRegisterCode ────────────────

    @Nested
    class VerifyRegisterCode {

        @Test
        void passesWithCorrectCodeAndCleansUpAllKeys() {
            String email = "user@example.com";
            String code = "123456";
            String hash = sha256("register:" + email + ":" + code + ":" + PROPS.codePepper());

            when(valueOps.get("email:register:code:" + email)).thenReturn(hash);

            assertThatCode(() -> service.verifyRegisterCode(email, code))
                    .doesNotThrowAnyException();

            verify(redisTemplate).delete("email:register:code:" + email);
            verify(redisTemplate).delete("email:register:attempts:" + email);
            verify(redisTemplate).delete("email:register:limit:" + email);
        }

        @Test
        void throwsWhenCodeExpired() {
            when(valueOps.get(anyString())).thenReturn(null);

            assertThatThrownBy(() -> service.verifyRegisterCode("user@example.com", "123456"))
                    .isInstanceOf(BadRequestException.class)
                    .satisfies(e -> assertThat(((BadRequestException) e).getErrorCode())
                            .isEqualTo(IdentityErrorCode.EMAIL_CODE_EXPIRED));
        }

        @Test
        void incrementsAttemptsAndSetsTtlOnFirstWrongAttempt() {
            String email = "user@example.com";
            when(valueOps.get("email:register:code:" + email)).thenReturn("stored-hash");
            when(valueOps.increment("email:register:attempts:" + email)).thenReturn(1L);

            assertThatThrownBy(() -> service.verifyRegisterCode(email, "wrong"))
                    .isInstanceOf(BadRequestException.class)
                    .satisfies(e -> assertThat(((BadRequestException) e).getErrorCode())
                            .isEqualTo(IdentityErrorCode.WRONG_VERIFICATION_CODE));

            verify(redisTemplate).expire("email:register:attempts:" + email, PROPS.codeTtl());
        }

        @Test
        void doesNotResetAttemptsTtlOnSubsequentWrongAttempts() {
            String email = "user@example.com";
            when(valueOps.get("email:register:code:" + email)).thenReturn("stored-hash");
            when(valueOps.increment("email:register:attempts:" + email)).thenReturn(2L);

            assertThatThrownBy(() -> service.verifyRegisterCode(email, "wrong"))
                    .isInstanceOf(BadRequestException.class);

            verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
        }

        @Test
        void locksOutAfterMaxAttemptsAndDeletesCodeButKeepsRateLimit() {
            String email = "user@example.com";
            when(valueOps.get("email:register:code:" + email)).thenReturn("stored-hash");
            when(valueOps.increment("email:register:attempts:" + email))
                    .thenReturn((long) PROPS.maxAttempts());

            assertThatThrownBy(() -> service.verifyRegisterCode(email, "wrong"))
                    .isInstanceOf(BadRequestException.class)
                    .satisfies(e -> assertThat(((BadRequestException) e).getErrorCode())
                            .isEqualTo(IdentityErrorCode.EMAIL_CODE_MAX_ATTEMPTS));

            verify(redisTemplate).delete("email:register:attempts:" + email);
            verify(redisTemplate).delete("email:register:code:" + email);
            // limitKey is NOT deleted on lockout — user must wait before requesting a new code
        }
    }

    // ──────────────── verifyLoginCode ────────────────

    @Nested
    class VerifyLoginCode {

        @Test
        void usesLoginPurposeInHash() {
            String email = "user@example.com";
            String code = "654321";
            String hash = sha256("login:" + email + ":" + code + ":" + PROPS.codePepper());

            when(valueOps.get("email:login:code:" + email)).thenReturn(hash);

            assertThatCode(() -> service.verifyLoginCode(email, code))
                    .doesNotThrowAnyException();
        }

        @Test
        void registerCodeDoesNotPassLoginVerification() {
            String email = "user@example.com";
            String code = "123456";
            // hash computed with "register" purpose
            String registerHash = sha256("register:" + email + ":" + code + ":" + PROPS.codePepper());

            // but stored under login key
            when(valueOps.get("email:login:code:" + email)).thenReturn(registerHash);
            when(valueOps.increment("email:login:attempts:" + email)).thenReturn(1L);

            assertThatThrownBy(() -> service.verifyLoginCode(email, code))
                    .isInstanceOf(BadRequestException.class)
                    .satisfies(e -> assertThat(((BadRequestException) e).getErrorCode())
                            .isEqualTo(IdentityErrorCode.WRONG_VERIFICATION_CODE));
        }
    }

    // ──────────────── helper ────────────────

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}