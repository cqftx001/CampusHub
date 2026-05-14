package com.campushub.identity.impl.service;

import com.campushub.identity.api.dto.EmailCodeLoginRequest;
import com.campushub.identity.api.dto.LoginRequest;
import com.campushub.identity.api.dto.RegisterRequest;
import com.campushub.identity.api.dto.SendEmailCodeRequest;
import com.campushub.identity.api.dto.VerifyEmailRequest;
import com.campushub.identity.api.vo.UserView;

public interface AuthService {

    UserView register(RegisterRequest request);

    UserView verifyEmail(VerifyEmailRequest request);

    void resendRegisterCode(SendEmailCodeRequest request);

    void sendEmailLoginCode(SendEmailCodeRequest request);

    TokenPair login(LoginRequest request);

    TokenPair loginWithEmailCode(EmailCodeLoginRequest request);

    TokenPair refreshToken(String refreshToken);

    void logout(String refreshToken);
}
