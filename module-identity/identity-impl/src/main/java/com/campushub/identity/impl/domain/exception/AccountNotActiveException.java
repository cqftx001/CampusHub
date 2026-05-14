package com.campushub.identity.impl.domain.exception;

import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.identity.impl.domain.enums.UserStatus;
import com.campushub.shared.exception.ForbiddenException;

/**
 * 账号未激活（UNVERIFIED / SUSPENDED / DELETED）时抛出 → HTTP 401。
 *
 * @author Kevin
 */
public class AccountNotActiveException extends ForbiddenException {

    public AccountNotActiveException(UserStatus status) {
        super(IdentityErrorCode.ACCOUNT_NOT_ACTIVE, "Account is " + status);
    }
}
