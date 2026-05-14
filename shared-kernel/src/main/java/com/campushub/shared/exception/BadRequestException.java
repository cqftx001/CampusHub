package com.campushub.shared.exception;

import com.campushub.shared.enums.ErrorCode;

public class BadRequestException extends BaseException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BadRequestException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
