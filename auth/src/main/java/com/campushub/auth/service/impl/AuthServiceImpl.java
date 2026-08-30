package com.campushub.auth.service.impl;

import com.campushub.auth.config.LoginSessionProperties;
import com.campushub.auth.domain.*;
import com.campushub.auth.dto.LoginClientContext;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RefreshTokenRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.event.AccountRegisteredEvent;
import com.campushub.auth.repository.*;
import com.campushub.auth.security.LoginAttemptLimiter;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.service.EmailVerificationService;
import com.campushub.auth.token.*;
import com.campushub.auth.vo.CurrentAccountView;
import com.campushub.auth.vo.LoginView;
import com.campushub.auth.vo.RegisterAccountView;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campushub.auth.error.RefreshTokenReuseDetectedException;
import com.campushub.auth.error.SessionRegistryRevocationFailedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    /*
     * 用于不存在的账户或不存在的密码凭证。
     * 即使 identifier 不存在，也执行一次 BCrypt 比对(BCrypt 故意设计得很慢)，
     * 减少基于响应时间进行账户枚举的可能性。
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";


    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RoleRepository roleRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtAccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final LoginSessionProperties sessionProperties;
    private final EmailVerificationService emailverificationService;
    private final AccessTokenRegistry registry;
    private final LoginAttemptLimiter loginAttemptLimiter;

    private final Clock clock;

    public AuthServiceImpl(
            AuthAccountRepository authAccountRepository,
            PasswordEncoder passwordEncoder,
            PasswordCredentialRepository passwordCredentialRepository,
            ApplicationEventPublisher eventPublisher,
            RoleRepository roleRepository,
            AccountRoleRepository accountRoleRepository,
            LoginSessionRepository loginSessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtAccessTokenIssuer accessTokenIssuer,
            RefreshTokenIssuer refreshTokenIssuer,
            LoginSessionProperties sessionProperties,
            EmailVerificationService emailverificationService,
            AccessTokenRegistry registry,
            LoginAttemptLimiter loginAttemptLimiter,
            Clock clock) {
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordCredentialRepository = passwordCredentialRepository;
        this.eventPublisher = eventPublisher;
        this.roleRepository = roleRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.sessionProperties = sessionProperties;
        this.emailverificationService = emailverificationService;
        this.registry = registry;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RegisterAccountView register(RegisterRequest request) {
        String username = normalizeIdentifier(request.username());
        String email = normalizeIdentifier(request.email());

        if(authAccountRepository.existsByUsername(username)) {
            throw new AuthException(AuthErrorCode.USERNAME_ALREADY_TAKEN);
        }

        if(authAccountRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        try{
            AuthAccount account = new AuthAccount(username, email);

            AuthAccount savedAccount = authAccountRepository.saveAndFlush(account);

            Instant registeredAt = Instant.now();

            PasswordCredential credential = new PasswordCredential(
                    savedAccount.getId(),
                    passwordEncoder.encode(request.password()),
                    registeredAt
            );

            passwordCredentialRepository.saveAndFlush(credential);

            Role userRole = roleRepository.findByCode(RoleCode.USER)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Default USER role has not been initialized"));

            accountRoleRepository.saveAndFlush(
                    new AccountRole(savedAccount.getId(),
                            userRole.getId(),
                            registeredAt)
            );

            eventPublisher.publishEvent(
              new AccountRegisteredEvent(
                      savedAccount.getId(),
                      savedAccount.getEmail()
              )
            );

            return toRegisterView(savedAccount);
        } catch(DataIntegrityViolationException e){
            throw new AuthException(
                    AuthErrorCode.IDENTIFIER_ALREADY_REGISTERED
            );
        }
    }

    @Override
    @Transactional
    public LoginView login(
            LoginRequest request,
            LoginClientContext loginClientContext
    ) {

        loginAttemptLimiter.assertAllowed(request.identifier(), loginClientContext.ipAddress());

        AuthAccount account;

        try {
            account = authenticate(request);
        } catch (AuthException e) {
            // Add limit record as invalid credentials
            if(e.getErrorCode() == AuthErrorCode.INVALID_CREDENTIALS) {
                loginAttemptLimiter.recordFailure(request.identifier(), loginClientContext.ipAddress());
            }
            throw e;
        }

        // Password Correct -> clear rate limit records
        loginAttemptLimiter.clearIdentifierFailures(request.identifier());

        ensureAccountCanLogin(account);

        Set<RoleCode> roles = accountRoleRepository.findRoleCodesByAccountId(account.getId());

        if (roles.isEmpty()) {
            throw new IllegalStateException("Account does not have any role" + account.getId());

        }

        Instant issuedAt = clock.instant();
        Instant sessionExpiresAt = issuedAt.plus(sessionProperties.ttl());
        LoginSession session = loginSessionRepository.saveAndFlush(
                new LoginSession(
                        account.getId(),
                        issuedAt,
                        sessionExpiresAt,
                        loginClientContext.userAgent(),
                        loginClientContext.ipAddress()
                ));

        IssuedRefreshToken issuedRefreshToken = refreshTokenIssuer.issue(
                session.getId(),
                issuedAt,
                sessionExpiresAt
        );

        refreshTokenRepository.saveAndFlush(issuedRefreshToken.refreshToken());

        IssuedAccessToken issuedAccessToken =
                accessTokenIssuer.issue(
                        account.getId(),
                        session.getId(),
                        roles,
                        issuedAt,
                        sessionExpiresAt
                );

        registry.register(
                account.getId(),
                session.getId(),
                issuedAccessToken.tokenId(),
                sessionExpiresAt
        );

        long expiresInSeconds = Duration.between(
                issuedAt,
                issuedAccessToken.expiresAt()
        ).toSeconds();

        return new LoginView(
                issuedAccessToken.value(),
                issuedRefreshToken.value(),
                "Bearer",
                expiresInSeconds,
                sessionExpiresAt
        );
    }

    @Override
    @Transactional(
            noRollbackFor = {
                    RefreshTokenReuseDetectedException.class,
                    SessionRegistryRevocationFailedException.class
            })
    public LoginView refresh(RefreshTokenRequest request) {
        Instant refreshedAt = clock.instant();

        String tokenHash = refreshTokenIssuer.hash(request.refreshToken());

        //先锁定session 避免deadlock
        UUID sessionId = refreshTokenRepository
                .findSessionIdByTokenHash(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        LoginSession session = loginSessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(this::invalidRefreshToken);

        if (!session.isActive(refreshedAt)) {
            throw invalidRefreshToken();
        }

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        if (!currentToken.getSessionId().equals(sessionId)) {
            throw invalidRefreshToken();
        }

        if (currentToken.getStatus() == RefreshTokenStatus.USED) {
            revokeCompromisedSession(
                    session,
                    refreshedAt
            );

            throw new RefreshTokenReuseDetectedException();
        }

        if (!currentToken.isUsable(refreshedAt)) {
            throw invalidRefreshToken();
        }

        AuthAccount account = authAccountRepository
                .findById(session.getAccountId())
                .orElseThrow(this::invalidRefreshToken);

        if (!account.isEnabled()){
            throw new AuthException(AuthErrorCode.ACCOUNT_DISABLED);
        }

        Set<RoleCode> roles = accountRoleRepository.findRoleCodesByAccountId(account.getId());
        if (roles.isEmpty()) {
            throw new IllegalStateException("Account does not have any role" + account.getId());
        }

        IssuedRefreshToken issuedRefreshToken = refreshTokenIssuer.issue(
                session.getId(),
                refreshedAt,
                session.getExpiresAt()
        );

        RefreshToken replacementToken = refreshTokenRepository.saveAndFlush(issuedRefreshToken.refreshToken());

        currentToken.markUsed(
                refreshedAt,
                replacementToken.getId()
        );

        session.markUsed(refreshedAt);

        IssuedAccessToken issuedAccessToken =
                accessTokenIssuer.issue(
                        account.getId(),
                        session.getId(),
                        roles,
                        refreshedAt,
                        session.getExpiresAt()
                );

        boolean accessTokenRotated = registry.rotate(
                account.getId(),
                session.getId(),
                issuedAccessToken.tokenId(),
                session.getExpiresAt()
        );

        if (!accessTokenRotated) {
            throw invalidRefreshToken();
        }

        long expiresInSeconds = Duration.between(
                refreshedAt,
                issuedAccessToken.expiresAt()
        ).toSeconds();

        return new LoginView(
                issuedAccessToken.value(),
                issuedRefreshToken.value(),
                "Bearer",
                expiresInSeconds,
                replacementToken.getExpiresAt()
        );
    }

    // 不会因为异常把安全状态回滚成 ACTIVE；
    @Override
    @Transactional(
            noRollbackFor =
                    SessionRegistryRevocationFailedException.class
    )
    public void logout(UUID accountId, UUID sessionId) {
        Instant revokedAt = clock.instant();

        LoginSession session = loginSessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(this::invalidAccessToken);

        if (!session.getAccountId().equals(accountId)) {
            throw invalidAccessToken();
        }

        refreshTokenRepository
                .findBySessionIdAndStatusForUpdate(
                        sessionId,
                        RefreshTokenStatus.ACTIVE
                )
                .ifPresent(token -> token.revoke(revokedAt));

        session.revoke(revokedAt);

        revokeOnlineSession(accountId, sessionId);
    }

    /**
     * 获取当前用户信息
     * @param accountId
     * @return CurrentAccountView
     */
    @Override
    public CurrentAccountView getCurrentAccount(UUID accountId) {
        AuthAccount account = authAccountRepository
                .findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.ACCESS_TOKEN_INVALID));

        if(!account.isEnabled()){
            throw new AuthException(AuthErrorCode.ACCOUNT_DISABLED);
        }

        return new CurrentAccountView(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getPhoneNumber(),
                account.getEmailVerifiedAt() != null,
                account.getPhoneVerifiedAt() != null
        );
    }


    // --- helper ---
    private void revokeCompromisedSession(
            LoginSession session,
            Instant revokedAt
    ) {
        refreshTokenRepository
                .findBySessionIdAndStatusForUpdate(
                        session.getId(),
                        RefreshTokenStatus.ACTIVE
                )
                .ifPresent(token -> token.revoke(revokedAt));

        session.revoke(revokedAt);

        revokeOnlineSession(session.getAccountId(), session.getId());
    }

    private void revokeOnlineSession(
            UUID accountId,
            UUID sessionId
    ) {
        try {
            registry.revoke(accountId, sessionId);
        } catch (AuthException exception) {
            if (exception.getErrorCode()
                    == AuthErrorCode.SESSION_REGISTRY_UNAVAILABLE) {
                throw new SessionRegistryRevocationFailedException();
            }

            throw exception;
        }
    }

    private AuthAccount authenticate(LoginRequest request){
        String identifier = normalizeIdentifier(request.identifier());

        Optional<AuthAccount> account = authAccountRepository.findByUsernameOrEmailOrPhoneNumber(
                identifier,
                identifier,
                identifier
        );

        if (account.isEmpty()) {
            performDummyPasswordCheck(request.password());
            throw invalidCredentials();
        }

        Optional<PasswordCredential> credential = passwordCredentialRepository.findById(account.orElseThrow().getId());

        if(credential.isEmpty()){
            performDummyPasswordCheck(request.password());
            throw invalidCredentials();
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                credential.orElseThrow().getPasswordHash()
        );

        if(!passwordMatches) throw invalidCredentials();

        return account.orElseThrow();
    }

    private void ensureAccountCanLogin(AuthAccount account){
        if(account.getEmail() != null && account.getEmailVerifiedAt() == null){
            emailverificationService.resend(account.getEmail());

            throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        if(!account.isEnabled()){
            throw new AuthException(AuthErrorCode.ACCOUNT_DISABLED);
        }
    }

    private void performDummyPasswordCheck(String rawPassword) {
        passwordEncoder.matches(rawPassword, DUMMY_PASSWORD_HASH);
    }

    private static String normalizeIdentifier(String identifier){
        if(identifier == null) return null;

        String normalized = identifier.strip();

        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private RegisterAccountView toRegisterView(AuthAccount account){
        return new RegisterAccountView(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                true
        );
    }

    private AuthException invalidCredentials(){
        return new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    private AuthException invalidRefreshToken() {
        return new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }

    private AuthException invalidAccessToken() {
        return new AuthException(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }

}
