package com.campushub.identity.impl.domain.exception;

import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.shared.exception.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(UUID userId){
        super(IdentityErrorCode.USER_NOT_FOUND, "User not found: " + userId);
    }

    public UserNotFoundException(String identifier){
        super(IdentityErrorCode.USER_NOT_FOUND, "User not found: " + identifier);
    }
}
