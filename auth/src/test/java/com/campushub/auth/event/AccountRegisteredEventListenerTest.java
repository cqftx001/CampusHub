package com.campushub.auth.event;

import com.campushub.auth.service.EmailVerificationService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AccountRegisteredEventListenerTest {

    @Test
    void mailFailureDoesNotEscapeRegistrationListener() {
        EmailVerificationService service = mock(EmailVerificationService.class);
        UUID accountId = UUID.randomUUID();
        doThrow(new IllegalStateException("mail unavailable"))
                .when(service)
                .sendInitialVerification(accountId, "campus.user@example.com");
        AccountRegisteredEventListener listener =
                new AccountRegisteredEventListener(service);

        assertThatCode(() -> listener.handle(new AccountRegisteredEvent(
                accountId,
                "campus.user@example.com"
        ))).doesNotThrowAnyException();
    }
}
