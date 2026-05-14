package com.campushub.shared.exception;

import com.campushub.shared.enums.ErrorCode;

public class ForbiddenException extends BaseException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
