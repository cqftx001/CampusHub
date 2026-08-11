package com.campushub.auth.error;

import com.campushub.shared.error.BaseException;

public class AuthException extends BaseException {

    public AuthException(AuthErrorCode authErrorCode) {
        super(authErrorCode);
    }

}
