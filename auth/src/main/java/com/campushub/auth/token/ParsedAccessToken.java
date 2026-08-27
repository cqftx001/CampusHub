package com.campushub.auth.token;

import com.campushub.auth.domain.RoleCode;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ParsedAccessToken(
        UUID accountId,
        UUID sessionId,
        Set<RoleCode> roles
) {

    public ParsedAccessToken {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(roles);

        roles = Set.copyOf(roles);

        if(roles.isEmpty()){
            throw new IllegalArgumentException("Access token roles cannot be empty");
        }
    }
}
