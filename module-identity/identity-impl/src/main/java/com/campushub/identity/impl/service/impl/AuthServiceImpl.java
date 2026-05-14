package com.campushub.identity.impl.service.impl;

import com.campushub.identity.api.dto.EmailCodeLoginRequest;
import com.campushub.identity.api.dto.LoginRequest;
import com.campushub.identity.api.dto.RegisterRequest;
import com.campushub.identity.api.dto.SendEmailCodeRequest;
import com.campushub.identity.api.dto.VerifyEmailRequest;
import com.campushub.identity.api.vo.AuthResponse;
import com.campushub.identity.api.vo.UserView;
import com.campushub.identity.impl.domain.entity.RefreshToken;
import com.campushub.identity.impl.domain.entity.User;
import com.campushub.identity.impl.domain.entity.UserProfile;
import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.identity.impl.domain.enums.UserStatus;
import com.campushub.identity.impl.domain.exception.*;
import com.campushub.identity.impl.domain.repository.RefreshTokenRepository;
import com.campushub.identity.impl.domain.repository.UserProfileRepository;
import com.campushub.identity.impl.domain.repository.UserRepository;
import com.campushub.identity.impl.service.AuthService;
import com.campushub.identity.impl.service.EmailVerificationService;
import com.campushub.identity.impl.service.TokenPair;
import com.campushub.security.config.JwtProperties;
import com.campushub.security.jwt.JwtUtils;
import com.campushub.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final EmailVerificationService emailVerificationService;

    // ==================== 注册 ====================

    /**
     * 用户注册流程：
     *   1. 校验邮箱和用户名是否已被占用
     *   2. 密码哈希（BCrypt）
     *   3. 创建 User entity，状态为 UNVERIFIED
     *   4. 保存到数据库
     *   5. 发送注册邮箱验证码
     *   6. 返回用户视图
     */
    @Override
    @Transactional
    public UserView register(RegisterRequest request) {
        // 1. 唯一性校验
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            log.info("Registration rejected: email already exists. email={}", email);
            throw new EmailAlreadyExistsException();
        }

        if (userRepository.existsByUsername(request.username())) {
            log.info("Registration rejected: username already exists. username={}", request.username());
            throw new UsernameAlreadyExistsException();
        }

        // 2. 创建本地用户
        User user = User.registerLocal(
                email,
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName()
        );
        user = userRepository.save(user);
        userProfileRepository.save(UserProfile.createForUser(user.getId()));
        emailVerificationService.sendRegisterCode(user.getEmail());
        log.info("User registered pending email verification: id={}, email={}", user.getId(), user.getEmail());

        return toUserView(user);
    }

    @Override
    @Transactional
    public UserView verifyEmail(VerifyEmailRequest request){
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.isActive()) {
            throw new ConflictException(IdentityErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        if (user.getStatus() != UserStatus.UNVERIFIED) {
            throw new AccountNotActiveException(user.getStatus());
        }

        emailVerificationService.verifyRegisterCode(email, request.code());
        user.activate();

        log.info("User email verified: id={}, email={}", user.getId(), user.getEmail());
        return toUserView(user);
    }

    @Override
    public void resendRegisterCode(SendEmailCodeRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.isActive()) {
            throw new ConflictException(IdentityErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        if (user.getStatus() != UserStatus.UNVERIFIED) {
            throw new AccountNotActiveException(user.getStatus());
        }

        emailVerificationService.sendRegisterCode(email);
    }

    @Override
    public void sendEmailLoginCode(SendEmailCodeRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(user -> emailVerificationService.sendLoginCode(email));
    }

    // ==================== 登录 ====================

    /**
     * 用户登录流程：
     *   1. 根据 identifier 查找用户（支持邮箱或用户名）
     *   2. 验证密码
     *   3. 检查账号状态
     *   4. 生成 access token + refresh token
     *   5. 保存 refresh token 到数据库
     *   6. 返回 token 对
     *
     * 安全要点：不区分"用户不存在"和"密码错误"，
     * 统一返回 InvalidCredentialsException，防止用户枚举攻击。
     */
    @Override
    @Transactional
    public TokenPair login(LoginRequest request) {
        // 1. 查找用户（邮箱或用户名）
        User user = findByIdentifier(request.identifier());

        // 2. OAuth 用户不能用密码登录
        if (user.isOAuthOnlyUser()) {
            throw new OAuthAccountOnlyException();
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 4. 检查账号状态
        if (!user.isActive()) {
            throw new AccountNotActiveException(user.getStatus());
        }

        // 5. 生成 token 对
        return generateTokenPair(user);
    }

    @Override
    @Transactional
    public TokenPair loginWithEmailCode(EmailCodeLoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new AccountNotActiveException(user.getStatus());
        }

        emailVerificationService.verifyLoginCode(email, request.code());
        return generateTokenPair(user);
    }

    // ==================== 刷新 Token ====================

    /**
     * Refresh token 续期流程：
     *   1. 根据 refresh token 字符串查找记录
     *   2. 验证是否有效（未过期 + 未被作废）
     *   3. 作废旧的 refresh token（一次性使用，防止重放攻击）
     *   4. 生成新的 access token + refresh token
     *
     * 为什么 refresh token 是一次性的？
     * 如果攻击者偷到了 refresh token，他用它续期一次。
     * 正常用户也用同一个 refresh token 续期 → 失败（已被作废）。
     * 正常用户发现异常 → 重新登录 → 攻击者的 token 也失效。
     * 这叫 "refresh token rotation"，是安全最佳实践。
     */
    @Override
    @Transactional
    public TokenPair refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        // 1. hash + 查找 hashed refresh token
        String rehashedRefreshToken = hashRefreshToken(refreshToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(rehashedRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException());

        // 2. 验证有效性
        if (!storedToken.isValid()) {
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(storedToken.getUserId()));

        if(!user.isActive()){
            throw new AccountNotActiveException(user.getStatus());
        }

        // 3. 作废旧 token
        storedToken.revoke("ROTATED");
        refreshTokenRepository.save(storedToken);

        return generateTokenPair(user, storedToken.getSessionId(), storedToken.getTokenFamilyId());
    }

    // ==================== logout ====================
    /**
     * 退出登录：作废 refresh token。
     * access token 没办法主动作废（JWT 特性），等它自然过期（30 分钟）。
     */
    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String hashedRefreshToken = hashRefreshToken(refreshToken);

        refreshTokenRepository.findByTokenHash(hashedRefreshToken)
                .ifPresent(token -> {
                    token.revoke("LOGOUT");
                    refreshTokenRepository.save(token);
                    log.info("User logged out, refresh token revoked");
                });
    }

    // ==================== 私有方法 ====================

    /**
     * Encode refresh token
     * Prevent storing data in plaintext.
     */
    private String hashRefreshToken(String refreshToken){
        if(refreshToken == null || refreshToken.isBlank()){
            throw new IllegalArgumentException("Refresh token must not be blank");
        }

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = refreshToken + ":" + jwtProperties.getRefreshTokenPepper();
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex){
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    /**
     * 根据 identifier（邮箱或用户名）查找用户。
     * 用 @ 符号判断是邮箱还是用户名。
     */
    private User findByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            String email = normalizeEmail(identifier);
            return userRepository.findByEmail(email)
                    .orElseThrow(InvalidCredentialsException::new);
        } else {
            return userRepository.findByUsername(identifier)
                    .orElseThrow(InvalidCredentialsException::new);
        }
    }

    /**
     * 生成 access token + refresh token 对。
     * 登录和续期都调这个方法，避免重复代码。
     */
    private TokenPair generateTokenPair(User user){
        return generateTokenPair(user, UUID.randomUUID(), UUID.randomUUID());
    }
    private TokenPair generateTokenPair(User user, UUID sessionId, UUID tokenFamilyId) {

        String roles = "ROLE_USER";

        // 1. 生成 access token（JWT）
        String accessToken = jwtUtils.generateToken(
                user.getId(),
                user.getEmail(),
                roles,
                sessionId
        );

        // 2. 生成 refresh token（随机字符串）+ Hash
        String rawRefreshToken = jwtUtils.generateRefreshToken();
        String hashedRefreshToken = hashRefreshToken(rawRefreshToken);


        // 3. 保存 refresh token 到数据库
        RefreshToken refreshToken = RefreshToken.issue(
                user.getId(),
                sessionId,
                tokenFamilyId,
                hashedRefreshToken,
                Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration())
        );
        refreshTokenRepository.save(refreshToken);

        // 4. 返回
        AuthResponse authResponse = AuthResponse.bearer(
                user.getId(),
                accessToken,
                jwtProperties.getAccessTokenExpiration(),
                roles
        );

        log.info("Token pair generated for user={}, sessionId={}", user.getId(), sessionId);

        return new TokenPair(authResponse, rawRefreshToken);
    }

    /**
     * Entity → VO 转换。
     */
    private UserView toUserView(User user) {
        return new UserView(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
