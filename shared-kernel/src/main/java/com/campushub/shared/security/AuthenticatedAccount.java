package com.campushub.shared.security;

import java.security.Principal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedAccount(
        UUID accountId,
        UUID sessionId,
        Set<String> roles
) implements Principal {

    public AuthenticatedAccount{
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(roles);

        roles = Set.copyOf(roles);
    }

    @Override
    public String getName() {
        return accountId.toString();
    }
}
