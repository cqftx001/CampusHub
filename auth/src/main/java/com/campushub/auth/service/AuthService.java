package com.campushub.auth.service;

import com.campushub.auth.dto.LoginClientContext;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RefreshTokenRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.vo.CurrentAccountView;
import com.campushub.auth.vo.LoginView;
import com.campushub.auth.vo.RegisterAccountView;

import java.util.UUID;

public interface AuthService {

    RegisterAccountView register(RegisterRequest request);

    LoginView login(
            LoginRequest request,
            LoginClientContext loginClientContext
    );

    LoginView refresh(RefreshTokenRequest request);

    CurrentAccountView getCurrentAccount(UUID accountId);

    void logout(UUID accountId, UUID sessionId);

    // void passwordChange(UUID accountId);
}
