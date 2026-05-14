package com.campushub.identity.impl.domain.exception;

import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.shared.exception.UnauthorizedException;

/**
 * Refresh token 无效或已过期时抛出 → HTTP 401。
 *
 * @author Kevin
 */
public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super(IdentityErrorCode.REFRESH_TOKEN_INVALID);
    }

    public InvalidRefreshTokenException(String detail) {
        super(IdentityErrorCode.REFRESH_TOKEN_INVALID, detail);
    }
}
