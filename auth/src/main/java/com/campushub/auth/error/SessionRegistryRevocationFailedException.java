package com.campushub.auth.error;

import jakarta.mail.Session;

public class SessionRegistryRevocationFailedException extends AuthException {

    public SessionRegistryRevocationFailedException() {
        super(AuthErrorCode.SESSION_REGISTRY_UNAVAILABLE);
    }
}
