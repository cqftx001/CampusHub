package com.campushub.auth.service;

import com.campushub.auth.dto.PasswordChangeRequest;
import com.campushub.auth.dto.PasswordResetConfirmRequest;
import com.campushub.auth.dto.PasswordResetRequest;

import java.util.UUID;

public interface PasswordService {

    void changePassword(UUID accountId, PasswordChangeRequest request);

    void requestPasswordReset(PasswordResetRequest request);

    void confirmPasswordReset(PasswordResetConfirmRequest request);
}
