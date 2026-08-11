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
