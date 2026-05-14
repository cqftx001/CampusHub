package com.campushub.identity.impl.domain.enums;


import com.campushub.shared.enums.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IdentityErrorCode implements ErrorCode {

    // -- user related --
    USER_NOT_FOUND("IDENTITY.USER_NOT_FOUND", "User not found", 404),
    EMAIL_ALREADY_EXISTS("IDENTITY.EMAIL_ALREADY_EXISTS", "Email already exists", 409),
    USERNAME_ALREADY_EXISTS("IDENTITY.USERNAME_ALREADY_EXISTS", "Username already exists", 409),

    // -- verification related --
    INVALID_CREDENTIALS("IDENTITY.INVALID_CREDENTIALS", "Invalid credentials", 401),
    INVALID_VERIFICATION_CODE("IDENTITY.INVALID_VERIFICATION_CODE", "Invalid verification code", 400),
    WRONG_VERIFICATION_CODE("IDENTITY.WRONG_VERIFICATION_CODE", "Wrong verification code", 400),
    REFRESH_TOKEN_INVALID("IDENTITY.REFRESH_TOKEN_INVALID", "Refresh token is invalid", 401),
    ACCOUNT_NOT_ACTIVE("IDENTITY.ACCOUNT_NOT_ACTIVE", "Account is not active", 403),
    EMAIL_CODE_RESEND_INTERVAL("IDENTITY.EMAIL_CODE_RESEND_INTERVAL", "Email code send too frequently", 400),
    EMAIL_CODE_EXPIRED("IDENTITY.EMAIL_CODE_EXPIRED", "Email code expired", 400),
    EMAIL_CODE_MAX_ATTEMPTS("IDENTITY.EMAIL_CODE_MAX_ATTEMPTS", "Email code max attempts reached", 400),
    EMAIL_ALREADY_VERIFIED("IDENTITY.EMAIL_ALREADY_VERIFIED", "Email is already verified", 409);

    private final String code;
    private final String message;
    private final int httpStatus;
}
