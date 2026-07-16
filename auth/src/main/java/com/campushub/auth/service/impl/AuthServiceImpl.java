package com.campushub.auth.service.impl;

import com.campushub.auth.domain.AuthAccount;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.repository.AuthAccountRepository;
import com.campushub.auth.security.JwtService;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.vo.AuthResponse;
import com.campushub.auth.vo.AuthUserView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AuthAccountRepository authAccountRepository,  PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalize(request.username());
        String email = normalize(request.email());

        validateUsername(username);
        validateEmail(email);

        String passwordHash = passwordEncoder.encode(request.password());
        AuthAccount account = new AuthAccount(
                username,
                email,
                passwordHash
        );

        try{
            AuthAccount savedAccount = authAccountRepository.saveAndFlush(account);
            return createAuthResponse(savedAccount);
        } catch (DataIntegrityViolationException ex){
            throw new AuthException(AuthErrorCode.IDENTIFIER_ALREADY_REGISTERED);
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = normalize(request.identifier());

        AuthAccount account =
                authAccountRepository.findByEmailOrUsername(identifier, identifier)
                        .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        return createAuthResponse(account);
    }

    @Override
    public AuthUserView currentUser(UUID accountId) {
        AuthAccount account = authAccountRepository.findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTHENTICATION_REQUIRED));

        return toUserView(account);
    }


    // --- helper ---
    private String normalize(String value){
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void validateUsername(String username) {
        if(authAccountRepository.existsByUsername(username)){
            throw new AuthException(AuthErrorCode.USERNAME_ALREADY_TAKEN);
        }
    }

    private void validateEmail(String email) {
        if(authAccountRepository.existsByEmail(email)){
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }
    }

    private AuthResponse createAuthResponse(AuthAccount account){
        String accessToken = jwtService.createAccessToken(account.getId());

        return new AuthResponse(
                accessToken,
                TOKEN_TYPE,
                jwtService.accessTokenTtlSeconds(),
                toUserView(account)
        );
    }

    private AuthUserView toUserView(AuthAccount account){
        return new AuthUserView(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getCreatedAt()
        );
    }
}
