package com.campushub.shared.security;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public record AuthenticatedAccount(
        UUID accountId
) implements Principal {

    public AuthenticatedAccount{
        Objects.requireNonNull(accountId);
    }

    @Override
    public String getName() {
        return accountId.toString();
    }
}
