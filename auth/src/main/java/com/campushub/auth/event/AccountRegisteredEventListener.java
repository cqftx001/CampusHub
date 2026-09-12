package com.campushub.auth.event;

import com.campushub.auth.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AccountRegisteredEventListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AccountRegisteredEventListener.class);

    private final EmailVerificationService emailVerificationService;

    public AccountRegisteredEventListener(
            EmailVerificationService emailVerificationService
    ) {
        this.emailVerificationService = emailVerificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AccountRegisteredEvent event) {
        try {
            emailVerificationService.sendInitialVerification(
                    event.accountId(),
                    event.email()
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Unable to send the initial email verification message for account {}",
                    event.accountId(),
                    exception
            );
        }
    }
}
