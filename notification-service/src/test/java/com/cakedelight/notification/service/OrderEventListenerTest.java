package com.cakedelight.notification.service;

import com.cakedelight.notification.event.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderEventListener listener;

    private final OrderCompletedEvent event =
            new OrderCompletedEvent(3L, new BigDecimal("849.00"), "COMPLETED", Instant.now());

    @Test
    void delegatesToNotificationService() {
        listener.handleOrderCompleted(event);

        verify(notificationService).sendOrderConfirmation(event);
    }

    @Test
    void recordsFailureAndRethrowsSoTheMessageIsRetried() {
        RuntimeException failure = new IllegalStateException("database down");
        doThrow(failure).when(notificationService).sendOrderConfirmation(event);

        assertThatThrownBy(() -> listener.handleOrderCompleted(event)).isSameAs(failure);
        verify(notificationService).recordFailure(eq(3L), any());
    }

    @Test
    void rejectsEventWithoutOrderId() {
        OrderCompletedEvent invalid = new OrderCompletedEvent(null, BigDecimal.ONE, "COMPLETED", null);

        assertThatThrownBy(() -> listener.handleOrderCompleted(invalid))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(notificationService);
    }
}
