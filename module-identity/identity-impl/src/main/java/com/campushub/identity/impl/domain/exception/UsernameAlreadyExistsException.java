package com.campushub.identity.impl.domain.exception;

import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.shared.exception.ConflictException;

public class UsernameAlreadyExistsException extends ConflictException {

    public UsernameAlreadyExistsException(){
        super(IdentityErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
