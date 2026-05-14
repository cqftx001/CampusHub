package com.campushub.identity.api.vo;

import java.time.Instant;
import java.util.UUID;

public record UserView(
        UUID id,
        String email,
        String displayName,
        String status,
        Instant createdAt
) {}
