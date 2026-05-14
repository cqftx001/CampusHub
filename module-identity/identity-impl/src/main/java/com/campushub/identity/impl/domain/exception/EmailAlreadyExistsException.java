package com.campushub.identity.impl.domain.exception;

import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.shared.exception.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {
    // 防止在message中暴露具体邮箱
    public EmailAlreadyExistsException(){
        super(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
