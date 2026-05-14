package com.campushub.messaging.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MessagingServiceImplTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void shouldPublishMessageSentEvent() {
        MessagingServiceImpl service = new MessagingServiceImpl(eventPublisher);
        service.send(1L, 2L, "hi");

        verify(eventPublisher).publishEvent(any(com.campushub.messaging.api.event.MessageSentEvent.class));
    }
}
