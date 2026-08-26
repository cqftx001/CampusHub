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
    SESSION_ALREADY_REVOKED(
            "AUTH_1009",
            "Session is already revoked",
            HttpStatus.BAD_REQUEST
    ),
    SESSION_ALREADY_EXPIRED(
            "AUTH_1010",
            "Session is already expired",
            HttpStatus.BAD_REQUEST
    ),
    REFRESH_TOKEN_LENGTH_INVALID(
            "AUTH_1011",
            "Refresh token hash must be a SHA-256 hexadecimal value.",
            HttpStatus.BAD_REQUEST
    ),
    REFRESH_TOKEN_EXPIRED(
           "AUTH_1012",
            "Refresh token is already expired",
            HttpStatus.BAD_REQUEST
    ),
    REFRESH_TOKEN_NON_USABLE(
            "AUTH_1013",
            "Only an active, unexpired refresh token can be used",
            HttpStatus.BAD_REQUEST
    )
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
