package com.campushub.identity.impl.domain.exception;

import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.shared.exception.BadRequestException;

/**
 * OAuth 账号尝试用密码登录时抛出 → HTTP 400。
 *
 * @author Kevin
 */
public class OAuthAccountOnlyException extends BadRequestException {

    public OAuthAccountOnlyException() {
        super(IdentityErrorCode.INVALID_CREDENTIALS,
                "This account uses OAuth login only.");
    }

    public OAuthAccountOnlyException(String customMessage){
        super(IdentityErrorCode.INVALID_CREDENTIALS,
                customMessage);
    }
}
