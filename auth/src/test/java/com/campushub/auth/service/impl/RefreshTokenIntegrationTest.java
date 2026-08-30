package com.campushub.auth.service.impl;

import com.campushub.auth.config.AuthCoreConfiguration;
import com.campushub.auth.config.AuthModuleConfiguration;
import com.campushub.auth.domain.AccountRole;
import com.campushub.auth.domain.AuthAccount;
import com.campushub.auth.domain.LoginSession;
import com.campushub.auth.domain.LoginSessionStatus;
import com.campushub.auth.domain.RefreshToken;
import com.campushub.auth.domain.RefreshTokenStatus;
import com.campushub.auth.domain.Role;
import com.campushub.auth.domain.RoleCode;
import com.campushub.auth.dto.RefreshTokenRequest;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.repository.AccountRoleRepository;
import com.campushub.auth.repository.AuthAccountRepository;
import com.campushub.auth.repository.LoginSessionRepository;
import com.campushub.auth.repository.RefreshTokenRepository;
import com.campushub.auth.repository.RoleRepository;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.service.EmailVerificationService;
import com.campushub.auth.token.AccessTokenRegistry;
import com.campushub.auth.token.IssuedRefreshToken;
import com.campushub.auth.token.JwtAccessTokenIssuer;
import com.campushub.auth.token.JwtSigningKeyProvider;
import com.campushub.auth.token.RefreshTokenIssuer;
import com.campushub.auth.vo.LoginView;
import com.campushub.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes =
                RefreshTokenIntegrationTest.TestApplication.class,
        properties = {
                "spring.flyway.enabled=false",
                "spring.datasource.url="
                        + "jdbc:h2:mem:refresh_token;"
                        + "DB_CLOSE_DELAY=-1;"
                        + "INIT=CREATE SCHEMA IF NOT EXISTS auth",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate."
                        + "hbm2ddl.create_namespaces=true",
                "spring.jpa.open-in-view=false",

                "campushub.auth.email-verification."
                        + "token-ttl=24h",
                "campushub.auth.email-verification."
                        + "resend-cooldown=60s",
                "campushub.auth.email-verification."
                        + "verification-url="
                        + "https://campushub.test/verify-email",
                "campushub.auth.email-verification."
                        + "from-address="
                        + "no-reply@campushub.test",

                "campushub.auth.jwt.issuer=campushub-test",
                "campushub.auth.jwt."
                        + "audience=campushub-api",
                "campushub.auth.jwt.secret="
                        + "Y2FtcHVzaHViLWRldi1qd3Qtc2VjcmV0"
                        + "LWNoYW5nZS1tZQ==",
                "campushub.auth.jwt."
                        + "access-token-ttl=15m",

                "campushub.auth.login-session.ttl=30d"
        }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RefreshTokenIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            AuthModuleConfiguration.class,
            AuthCoreConfiguration.class,
            AuthServiceImpl.class,
            RefreshTokenIssuer.class,
            JwtAccessTokenIssuer.class,
            JwtSigningKeyProvider.class
    })
    static class TestApplication {
    }

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private AccessTokenRegistry accessTokenRegistry;

    private final AuthService authService;
    private final AuthAccountRepository authAccountRepository;
    private final RoleRepository roleRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final Clock clock;

    @Autowired
    RefreshTokenIntegrationTest(
            AuthService authService,
            AuthAccountRepository authAccountRepository,
            RoleRepository roleRepository,
            AccountRoleRepository accountRoleRepository,
            LoginSessionRepository loginSessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenIssuer refreshTokenIssuer,
            Clock clock
    ) {
        this.authService = authService;
        this.authAccountRepository =
                authAccountRepository;
        this.roleRepository = roleRepository;
        this.accountRoleRepository =
                accountRoleRepository;
        this.loginSessionRepository =
                loginSessionRepository;
        this.refreshTokenRepository =
                refreshTokenRepository;
        this.refreshTokenIssuer =
                refreshTokenIssuer;
        this.clock = clock;
    }

    @BeforeEach
    void cleanDatabase() {
        lenient().when(accessTokenRegistry.rotate(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class),
                any(Instant.class)
        )).thenReturn(true);

        refreshTokenRepository.deleteAll();
        loginSessionRepository.deleteAll();
        accountRoleRepository.deleteAll();
        roleRepository.deleteAll();
        authAccountRepository.deleteAll();
    }

    @Test
    void activeRefreshTokenIsRotated() {
        RefreshFixture fixture = createFixture();

        LoginView result = authService.refresh(
                new RefreshTokenRequest(
                        fixture.rawToken()
                )
        );

        assertThat(result.accessToken()).isNotBlank();

        assertThat(result.refreshToken())
                .isNotBlank()
                .isNotEqualTo(fixture.rawToken());

        assertThat(result.tokenType())
                .isEqualTo("Bearer");

        assertThat(result.expiresInSeconds())
                .isEqualTo(900);

        assertThat(result.refreshTokenExpiresAt())
                .isEqualTo(
                        fixture.sessionExpiresAt()
                );

        RefreshToken oldToken =
                refreshTokenRepository
                        .findById(
                                fixture.refreshTokenId()
                        )
                        .orElseThrow();

        assertThat(oldToken.getStatus())
                .isEqualTo(
                        RefreshTokenStatus.USED
                );

        assertThat(oldToken.getUsedAt())
                .isNotNull();

        assertThat(oldToken.getReplacedByTokenId())
                .isNotNull();

        RefreshToken replacementToken =
                refreshTokenRepository
                        .findById(
                                oldToken
                                        .getReplacedByTokenId()
                        )
                        .orElseThrow();

        assertThat(replacementToken.getStatus())
                .isEqualTo(
                        RefreshTokenStatus.ACTIVE
                );

        assertThat(replacementToken.getSessionId())
                .isEqualTo(fixture.sessionId());

        assertThat(replacementToken.getTokenHash())
                .isEqualTo(
                        refreshTokenIssuer.hash(
                                result.refreshToken()
                        )
                );
    }

    @Test
    void reusedRefreshTokenRevokesEntireSession() {
        RefreshFixture fixture = createFixture();

        authService.refresh(
                new RefreshTokenRequest(
                        fixture.rawToken()
                )
        );

        assertThatThrownBy(() ->
                authService.refresh(
                        new RefreshTokenRequest(
                                fixture.rawToken()
                        )
                )
        )
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode
                                        .REFRESH_TOKEN_INVALID
                        )
                );

        LoginSession session =
                loginSessionRepository
                        .findById(fixture.sessionId())
                        .orElseThrow();

        assertThat(session.getStatus())
                .isEqualTo(
                        LoginSessionStatus.REVOKED
                );

        List<RefreshToken> storedTokens =
                refreshTokenRepository.findAll();

        assertThat(storedTokens).hasSize(2);

        assertThat(storedTokens)
                .filteredOn(token ->
                        token.getStatus()
                                == RefreshTokenStatus.USED
                )
                .hasSize(1);

        assertThat(storedTokens)
                .filteredOn(token ->
                        token.getStatus()
                                == RefreshTokenStatus.REVOKED
                )
                .hasSize(1);

        assertThat(storedTokens)
                .noneMatch(token ->
                        token.getStatus()
                                == RefreshTokenStatus.ACTIVE
                );

        verify(accessTokenRegistry).revoke(
                fixture.accountId(),
                fixture.sessionId()
        );
    }

    @Test
    void replayDatabaseRevocationIsCommittedWhenRedisFails() {
        RefreshFixture fixture = createFixture();

        authService.refresh(
                new RefreshTokenRequest(
                        fixture.rawToken()
                )
        );

        when(accessTokenRegistry.revoke(
                fixture.accountId(),
                fixture.sessionId()
        )).thenThrow(
                new AuthException(
                        AuthErrorCode
                                .SESSION_REGISTRY_UNAVAILABLE
                )
        );

        assertThatThrownBy(() ->
                authService.refresh(
                        new RefreshTokenRequest(
                                fixture.rawToken()
                        )
                )
        )
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode
                                        .SESSION_REGISTRY_UNAVAILABLE
                        )
                );

        LoginSession session =
                loginSessionRepository
                        .findById(fixture.sessionId())
                        .orElseThrow();

        assertThat(session.getStatus())
                .isEqualTo(
                        LoginSessionStatus.REVOKED
                );

        List<RefreshToken> storedTokens =
                refreshTokenRepository.findAll();

        assertThat(storedTokens)
                .filteredOn(token ->
                        token.getStatus()
                                == RefreshTokenStatus.USED
                )
                .hasSize(1);

        assertThat(storedTokens)
                .filteredOn(token ->
                        token.getStatus()
                                == RefreshTokenStatus.REVOKED
                )
                .hasSize(1);

        assertThat(storedTokens)
                .noneMatch(token ->
                        token.getStatus()
                                == RefreshTokenStatus.ACTIVE
                );
    }

    @Test
    void unknownRefreshTokenIsRejected() {
        assertThatThrownBy(() ->
                authService.refresh(
                        new RefreshTokenRequest(
                                "unknown-refresh-token"
                        )
                )
        )
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode
                                        .REFRESH_TOKEN_INVALID
                        )
                );
    }

    @Test
    void concurrentReuseRevokesTheRotatedSession()
            throws Exception {

        RefreshFixture fixture = createFixture();

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<RefreshAttempt> first =
                    executor.submit(
                            () -> refreshWhenReleased(
                                    fixture.rawToken(),
                                    ready,
                                    start
                            )
                    );

            Future<RefreshAttempt> second =
                    executor.submit(
                            () -> refreshWhenReleased(
                                    fixture.rawToken(),
                                    ready,
                                    start
                            )
                    );

            assertThat(
                    ready.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            start.countDown();

            List<RefreshAttempt> attempts =
                    List.of(
                            first.get(
                                    10,
                                    TimeUnit.SECONDS
                            ),
                            second.get(
                                    10,
                                    TimeUnit.SECONDS
                            )
                    );

            assertThat(attempts)
                    .filteredOn(
                            RefreshAttempt::succeeded
                    )
                    .hasSize(1);

            assertThat(attempts)
                    .filteredOn(attempt ->
                            !attempt.succeeded()
                    )
                    .extracting(
                            RefreshAttempt::errorCode
                    )
                    .containsExactly(
                            AuthErrorCode
                                    .REFRESH_TOKEN_INVALID
                    );

            List<RefreshToken> storedTokens =
                    refreshTokenRepository.findAll();

            assertThat(storedTokens).hasSize(2);

            RefreshToken usedToken =
                    storedTokens.stream()
                            .filter(token ->
                                    token.getStatus()
                                            == RefreshTokenStatus
                                            .USED
                            )
                            .findFirst()
                            .orElseThrow();

            RefreshToken revokedReplacement =
                    storedTokens.stream()
                            .filter(token ->
                                    token.getStatus()
                                            == RefreshTokenStatus
                                            .REVOKED
                            )
                            .findFirst()
                            .orElseThrow();

            assertThat(
                    usedToken.getReplacedByTokenId()
            ).isEqualTo(
                    revokedReplacement.getId()
            );

            assertThat(storedTokens)
                    .noneMatch(token ->
                            token.getStatus()
                                    == RefreshTokenStatus.ACTIVE
                    );

            LoginSession session =
                    loginSessionRepository
                            .findById(
                                    fixture.sessionId()
                            )
                            .orElseThrow();

            assertThat(session.getStatus())
                    .isEqualTo(
                            LoginSessionStatus.REVOKED
                    );

            verify(accessTokenRegistry).revoke(
                    fixture.accountId(),
                    fixture.sessionId()
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void refreshTokenCannotBeUsedAfterSessionIsRevoked() {
        RefreshFixture fixture = createFixture();

        LoginSession session =
                loginSessionRepository
                        .findById(fixture.sessionId())
                        .orElseThrow();

        session.revoke(clock.instant());

        loginSessionRepository.saveAndFlush(session);

        assertThatThrownBy(() ->
                authService.refresh(
                        new RefreshTokenRequest(
                                fixture.rawToken()
                        )
                )
        )
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode
                                        .REFRESH_TOKEN_INVALID
                        )
                );

        RefreshToken originalToken =
                refreshTokenRepository
                        .findById(
                                fixture.refreshTokenId()
                        )
                        .orElseThrow();

        assertThat(originalToken.getStatus())
                .isEqualTo(
                        RefreshTokenStatus.ACTIVE
                );

        assertThat(refreshTokenRepository.count())
                .isEqualTo(1);
    }

    @Test
    void expiredRefreshTokenCannotBeUsed() {
        Instant issuedAt =
                clock.instant()
                        .minus(
                                2,
                                ChronoUnit.HOURS
                        )
                        .truncatedTo(
                                ChronoUnit.MILLIS
                        );

        Instant expiredAt =
                issuedAt.plus(
                        1,
                        ChronoUnit.HOURS
                );

        RefreshFixture fixture =
                createFixture(
                        issuedAt,
                        expiredAt
                );

        assertThatThrownBy(() ->
                authService.refresh(
                        new RefreshTokenRequest(
                                fixture.rawToken()
                        )
                )
        )
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode
                                        .REFRESH_TOKEN_INVALID
                        )
                );

        RefreshToken originalToken =
                refreshTokenRepository
                        .findById(
                                fixture.refreshTokenId()
                        )
                        .orElseThrow();

        assertThat(originalToken.getStatus())
                .isEqualTo(
                        RefreshTokenStatus.ACTIVE
                );

        assertThat(refreshTokenRepository.count())
                .isEqualTo(1);
    }

    @Test
    void disabledAccountCannotRefreshToken() {
        RefreshFixture fixture = createFixture();

        AuthAccount account =
                authAccountRepository
                        .findById(fixture.accountId())
                        .orElseThrow();

        account.disable();

        authAccountRepository.saveAndFlush(account);

        assertThatThrownBy(() ->
                authService.refresh(
                        new RefreshTokenRequest(
                                fixture.rawToken()
                        )
                )
        )
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode.ACCOUNT_DISABLED
                        )
                );

        RefreshToken originalToken =
                refreshTokenRepository
                        .findById(
                                fixture.refreshTokenId()
                        )
                        .orElseThrow();

        assertThat(originalToken.getStatus())
                .isEqualTo(
                        RefreshTokenStatus.ACTIVE
                );

        assertThat(refreshTokenRepository.count())
                .isEqualTo(1);
    }

    @Test
    void missingOnlineSessionRollsBackRefreshRotation() {
        RefreshFixture fixture = createFixture();

        when(accessTokenRegistry.rotate(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class),
                any(Instant.class)
        )).thenReturn(false);

        assertThatThrownBy(() ->
                authService.refresh(
                        new RefreshTokenRequest(
                                fixture.rawToken()
                        )
                )
        )
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode
                                        .REFRESH_TOKEN_INVALID
                        )
                );

        RefreshToken originalToken =
                refreshTokenRepository
                        .findById(
                                fixture.refreshTokenId()
                        )
                        .orElseThrow();

        assertThat(originalToken.getStatus())
                .isEqualTo(
                        RefreshTokenStatus.ACTIVE
                );

        assertThat(refreshTokenRepository.count())
                .isEqualTo(1);
    }

    @Test
    void logoutRevokesSessionRefreshTokenAndRegistry() {
        RefreshFixture fixture = createFixture();

        authService.logout(
                fixture.accountId(),
                fixture.sessionId()
        );

        LoginSession session =
                loginSessionRepository
                        .findById(fixture.sessionId())
                        .orElseThrow();

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findById(
                                fixture.refreshTokenId()
                        )
                        .orElseThrow();

        assertThat(session.getStatus())
                .isEqualTo(
                        LoginSessionStatus.REVOKED
                );

        assertThat(refreshToken.getStatus())
                .isEqualTo(
                        RefreshTokenStatus.REVOKED
                );

        verify(accessTokenRegistry).revoke(
                fixture.accountId(),
                fixture.sessionId()
        );
    }

    private RefreshFixture createFixture() {
        Instant issuedAt =
                clock.instant()
                        .truncatedTo(
                                ChronoUnit.MILLIS
                        );

        return createFixture(
                issuedAt,
                issuedAt.plus(
                        30,
                        ChronoUnit.DAYS
                )
        );
    }

    private RefreshFixture createFixture(
            Instant issuedAt,
            Instant sessionExpiresAt
    ) {
        AuthAccount account =
                new AuthAccount(
                        "refresh.user",
                        "refresh@example.com"
                );

        account.verifyEmail(issuedAt);

        account =
                authAccountRepository
                        .saveAndFlush(account);

        Role userRole =
                roleRepository.saveAndFlush(
                        new Role(RoleCode.USER)
                );

        accountRoleRepository.saveAndFlush(
                new AccountRole(
                        account.getId(),
                        userRole.getId(),
                        issuedAt
                )
        );

        LoginSession session =
                loginSessionRepository.saveAndFlush(
                        new LoginSession(
                                account.getId(),
                                issuedAt,
                                sessionExpiresAt,
                                "integration-test",
                                "127.0.0.1"
                        )
                );

        IssuedRefreshToken issuedRefreshToken =
                refreshTokenIssuer.issue(
                        session.getId(),
                        issuedAt,
                        sessionExpiresAt
                );

        RefreshToken savedToken =
                refreshTokenRepository.saveAndFlush(
                        issuedRefreshToken
                                .refreshToken()
                );

        return new RefreshFixture(
                issuedRefreshToken.value(),
                savedToken.getId(),
                account.getId(),
                session.getId(),
                sessionExpiresAt
        );
    }

    private RefreshAttempt refreshWhenReleased(
            String rawToken,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();

        try {
            if (!start.await(
                    5,
                    TimeUnit.SECONDS
            )) {
                throw new IllegalStateException(
                        "Concurrent refresh "
                                + "did not start"
                );
            }

            LoginView result =
                    authService.refresh(
                            new RefreshTokenRequest(
                                    rawToken
                            )
                    );

            return new RefreshAttempt(
                    result,
                    null
            );
        } catch (AuthException exception) {
            return new RefreshAttempt(
                    null,
                    exception.getErrorCode()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Concurrent refresh "
                            + "was interrupted",
                    exception
            );
        }
    }

    private record RefreshFixture(
            String rawToken,
            UUID refreshTokenId,
            UUID accountId,
            UUID sessionId,
            Instant sessionExpiresAt
    ) {
    }

    private record RefreshAttempt(
            LoginView result,
            ErrorCode errorCode
    ) {
        boolean succeeded() {
            return result != null;
        }
    }
}