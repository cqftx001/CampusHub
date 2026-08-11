package com.campushub.user.error;

import com.campushub.shared.error.BaseException;

public class UserException extends BaseException {

    public UserException(UserErrorCode errorCode){
        super(errorCode);
    }
}
