package com.campushub.auth.service.impl;

import com.campushub.auth.domain.AuthAccount;
import com.campushub.auth.domain.PasswordCredential;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.repository.AuthAccountRepository;
import com.campushub.auth.repository.PasswordCredentialRepository;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.vo.RegisterAccountView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordCredentialRepository passwordCredentialRepository;

    public AuthServiceImpl(AuthAccountRepository authAccountRepository, PasswordEncoder passwordEncoder, PasswordCredentialRepository passwordCredentialRepository) {
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordCredentialRepository = passwordCredentialRepository;
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

            PasswordCredential credential = new PasswordCredential(
                    savedAccount.getId(),
                    passwordEncoder.encode(request.password()),
                    Instant.now()
            );

            passwordCredentialRepository.saveAndFlush(credential);

            return toRegisterView(savedAccount);
        } catch(DataIntegrityViolationException e){
            throw new AuthException(
                    AuthErrorCode.IDENTIFIER_ALREADY_REGISTERED
            );
        }
    }


    // --- helper ---
    private String normalizeIdentifier(String identifier){
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

//    private AuthAccountView toView(AuthAccount account){
//        return new AuthAccountView(
//                account.getId(),
//                account.getUsername(),
//                account.getEmail(),
//                account.getPhoneNumber(),
//                account.isEnabled(),
//                account.getEmailVerifiedAt(),
//                account.getPhoneVerifiedAt(),
//                account.getCreatedAt()
//        );
//    }
}
