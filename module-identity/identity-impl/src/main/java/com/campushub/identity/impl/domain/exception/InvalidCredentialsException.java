package com.campushub.identity.impl.domain.exception;

import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.shared.exception.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException(){
        super(IdentityErrorCode.INVALID_CREDENTIALS);
    }
}
