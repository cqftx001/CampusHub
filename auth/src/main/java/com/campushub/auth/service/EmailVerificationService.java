package com.campushub.auth.service;


import java.util.UUID;

public interface EmailVerificationService {

    void sendInitialVerification(
            UUID accountId,
            String email);

    void confirm(String token);

    void resend(String email);

}
