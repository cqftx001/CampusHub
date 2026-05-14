package com.campushub.identity.impl.service;

public interface EmailVerificationService {

    void sendRegisterCode(String email);

    void verifyRegisterCode(String email, String code);

    void sendLoginCode(String email);

    void verifyLoginCode(String email, String code);
}
