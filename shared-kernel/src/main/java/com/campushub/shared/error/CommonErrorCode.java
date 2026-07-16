package com.campushub.shared.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

    SUCCESS("0000", "Success", HttpStatus.OK),

    INVALID_REQUEST("COMMON_1000", "Invalid request", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("COMMON_1001", "Validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON_1002", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON_1003", "Forbidden", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("COMMON_1004", "Resource not found", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("COMMON_9999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(String code, String message, HttpStatus httpStatus) {
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