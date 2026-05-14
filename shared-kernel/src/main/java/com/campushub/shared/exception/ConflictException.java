package com.campushub.shared.exception;

import com.campushub.shared.enums.ErrorCode;

public class ConflictException extends BaseException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
