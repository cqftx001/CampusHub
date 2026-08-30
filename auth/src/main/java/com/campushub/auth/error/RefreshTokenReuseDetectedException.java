package com.campushub.auth.error;

public final class RefreshTokenReuseDetectedException extends AuthException{

    public RefreshTokenReuseDetectedException() {
        super(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
}
