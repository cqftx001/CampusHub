package com.campushub.auth.vo;

import java.util.UUID;

public record CurrentAccountView(
        UUID accountId,
        String username,
        String email,
        String phoneNumber,
        boolean emailVerified,
        boolean phoneVerified
) {
}
