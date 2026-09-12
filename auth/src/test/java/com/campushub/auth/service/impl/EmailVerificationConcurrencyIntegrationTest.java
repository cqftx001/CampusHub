package com.campushub.auth.service.impl;

import com.campushub.auth.config.AuthModuleConfiguration;
import com.campushub.auth.domain.AuthAccount;
import com.campushub.auth.repository.AuthAccountRepository;
import com.campushub.auth.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.campushub.auth.utils.AuthInputNormalizer;
import com.campushub.auth.utils.SecureTokenGenerator;
import com.campushub.auth.utils.Sha256Hasher;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = EmailVerificationConcurrencyIntegrationTest.TestApplication.class,
        properties = {
                "spring.flyway.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:email-verification;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS auth",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
                "campushub.auth.email-verification.token-ttl=24h",
                "campushub.auth.email-verification.resend-cooldown=60s",
                "campushub.auth.email-verification.verification-url=https://campushub.test/verify-email",
                "campushub.auth.email-verification.from-address=no-reply@campushub.test",
                "campushub.auth.jwt.issuer=campushub-test",
                "campushub.auth.jwt.secret=Y2FtcHVzaHViLWRldi1qd3Qtc2VjcmV0LWNoYW5nZS1tZQ==",
                "campushub.auth.jwt.access-token-ttl=15m",
                "campushub.auth.login-session.ttl=30d",
                "campushub.auth.jwt.issuer=campushub-test",
                "campushub.auth.jwt.audience=campushub-api",
                "campushub.auth.jwt.secret="
                        + "Y2FtcHVzaHViLWRldi1qd3Qtc2VjcmV0"
                        + "LWNoYW5nZS1tZQ==",
                "campushub.auth.jwt.access-token-ttl=15m",
        }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailVerificationConcurrencyIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            AuthModuleConfiguration.class,
            EmailVerificationServiceImpl.class,
            SecureTokenGenerator.class,
            Sha256Hasher.class,
            AuthInputNormalizer.class
    })
    static class TestApplication {
    }

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private JavaMailSender javaMailSender;

    private ValueOperations<String, String> valueOperations;

    private final AuthAccountRepository authAccountRepository;
    private final EmailVerificationService emailVerificationService;

    @Autowired
    EmailVerificationConcurrencyIntegrationTest(
            AuthAccountRepository authAccountRepository,
            EmailVerificationService emailVerificationService
    ) {
        this.authAccountRepository = authAccountRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doReturn(1L).when(stringRedisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        );
    }

    @Test
    void concurrentDuplicateConfirmRequestsAreIdempotent() throws Exception {
        AuthAccount savedAccount = authAccountRepository.saveAndFlush(
                new AuthAccount("concurrent.user", "concurrent@example.com")
        );
        UUID accountId = savedAccount.getId();
        when(valueOperations.get(anyString())).thenReturn(accountId.toString());

        runConcurrently(() -> emailVerificationService.confirm(
                "shared-current-token"
        ));

        Optional<AuthAccount> verifiedAccount = authAccountRepository.findById(accountId);
        assertThat(verifiedAccount).isPresent();
        assertThat(verifiedAccount.orElseThrow().isEnabled()).isTrue();
        assertThat(verifiedAccount.orElseThrow().getEmailVerifiedAt()).isNotNull();
        assertThat(verifiedAccount.orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    void concurrentDuplicateResendRequestsSendOneMessage() throws Exception {
        authAccountRepository.saveAndFlush(
                new AuthAccount("resend.user", "resend@example.com")
        );
        AtomicBoolean cooldownAcquired = new AtomicBoolean();
        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                eq(Duration.ofSeconds(60))
        )).thenAnswer(invocation -> cooldownAcquired.compareAndSet(false, true));

        runConcurrently(() -> emailVerificationService.resend(
                "resend@example.com"
        ));

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    private void runConcurrently(Runnable action) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> runWhenReleased(ready, start, action));
            Future<?> second = executor.submit(() -> runWhenReleased(ready, start, action));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private void runWhenReleased(
            CountDownLatch ready,
            CountDownLatch start,
            Runnable action
    ) {
        try {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent confirmation did not start");
            }
            action.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent confirmation was interrupted", exception);
        }
    }
}
