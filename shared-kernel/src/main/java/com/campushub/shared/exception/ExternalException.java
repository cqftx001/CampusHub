package com.campushub.shared.exception;

import com.campushub.shared.enums.ErrorCode;

public class ExternalException extends BaseException {

    public ExternalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public ExternalException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
