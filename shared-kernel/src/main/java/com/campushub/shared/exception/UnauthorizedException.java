package com.campushub.shared.exception;

import com.campushub.shared.enums.ErrorCode;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
