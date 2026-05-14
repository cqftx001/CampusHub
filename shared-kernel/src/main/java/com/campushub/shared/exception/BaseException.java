package com.campushub.shared.exception;

import com.campushub.shared.enums.ErrorCode;

public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected BaseException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    protected BaseException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
