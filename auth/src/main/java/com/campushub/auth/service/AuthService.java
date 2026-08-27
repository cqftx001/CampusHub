package com.campushub.auth.service;

import com.campushub.auth.dto.LoginClientContext;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RefreshTokenRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.vo.LoginView;
import com.campushub.auth.vo.RegisterAccountView;

public interface AuthService {

    RegisterAccountView register(RegisterRequest request);

    LoginView login(
            LoginRequest request,
            LoginClientContext loginClientContext
    );

    LoginView refresh(RefreshTokenRequest request);
}
