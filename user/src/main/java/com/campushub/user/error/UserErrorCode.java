package com.campushub.user.error;

import com.campushub.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

    PROFILE_UPDATE_CONFLICT(
            "USER_1001",
            "User profile was updated concurrently",
            HttpStatus.CONFLICT
    );

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    UserErrorCode(
            String code,
            String message,
            HttpStatus httpStatus
    ) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
