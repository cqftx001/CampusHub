package com.campushub.shared.exception;

import com.campushub.shared.enums.ErrorCode;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String resourceName, Object resourceId) {
        super(errorCode, resourceName + " not found with id: " + resourceId);
    }
}
