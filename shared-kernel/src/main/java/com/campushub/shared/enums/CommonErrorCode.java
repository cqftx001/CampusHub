package com.campushub.shared.enums;

public enum CommonErrorCode implements ErrorCode {
    SUCCESS("COMMON.SUCCESS", "Success", 200),

    BAD_REQUEST("COMMON.BAD_REQUEST", "Bad request", 400),
    VALIDATION_ERROR("COMMON.VALIDATION_ERROR", "Request validation failed", 400),
    UNAUTHORIZED("COMMON.UNAUTHORIZED", "Authentication required", 401),
    FORBIDDEN("COMMON.FORBIDDEN", "Permission denied", 403),
    NOT_FOUND("COMMON.NOT_FOUND", "Resource not found", 404),
    CONFLICT("COMMON.CONFLICT", "Resource conflict", 409),
    DUPLICATE_REQUEST("COMMON.DUPLICATE_REQUEST", "Duplicate request", 409),

    NOT_IMPLEMENTED("COMMON.NOT_IMPLEMENTED", "This feature is not implemented yet", 501),
    EXTERNAL_SERVICE_ERROR("COMMON.EXTERNAL_SERVICE_ERROR", "External service unavailable", 502),
    INTERNAL_ERROR("COMMON.INTERNAL_ERROR", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    CommonErrorCode(String code, String message, int httpStatus) {
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
    public int getHttpStatus() {
        return httpStatus;
    }
}
