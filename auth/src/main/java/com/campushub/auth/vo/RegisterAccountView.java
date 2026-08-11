package com.campushub.auth.vo;

import java.util.UUID;

public record RegisterAccountView(
        UUID accountId,
        String username,
        String email,
        boolean emailVerificationRequired
) {
}
