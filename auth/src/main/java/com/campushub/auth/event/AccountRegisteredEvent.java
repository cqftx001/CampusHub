package com.campushub.auth.event;

import java.util.UUID;

public record AccountRegisteredEvent(
        UUID accountId,
        String email
) {
}
