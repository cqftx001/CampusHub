package com.campushub.auth.error;

import com.campushub.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    EMAIL_ALREADY_REGISTERED(
            "AUTH_1001",
            "Email is already registered",
            HttpStatus.CONFLICT
    ),

    USERNAME_ALREADY_TAKEN(
            "AUTH_1002",
            "Username is already taken",
            HttpStatus.CONFLICT
    ),

    IDENTIFIER_ALREADY_REGISTERED(
            "AUTH_1003",
            "Username or Email is already registered",
            HttpStatus.CONFLICT
    ),

    EMAIL_VERIFICATION_TOKEN_INVALID(
            "AUTH_1006",
            "Email verification token is invalid or expired",
            HttpStatus.BAD_REQUEST
    ),

    EMAIL_VERIFICATION_UNAVAILABLE(
            "AUTH_1007",
            "Email verification is temporarily unavailable",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    EMAIL_VERIFICATION_REQUIRED(
            "AUTH_1008",
            "Email verification is required",
            HttpStatus.FORBIDDEN
    ),
    ;


    private String code;
    private String description;
    private HttpStatus httpStatus;

    AuthErrorCode(String code, String description, HttpStatus httpStatus) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.description;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }
}
