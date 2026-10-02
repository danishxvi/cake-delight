package com.cakedelight.notification.service;

import com.cakedelight.notification.config.RabbitMQConfig;
import com.cakedelight.notification.event.OrderCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order-completion events published by order-service.
 *
 * If processing throws, the listener container retries the message (see
 * spring.rabbitmq.listener.simple.retry.* in application.properties) and then
 * moves it to the dead-letter queue.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.orderId() == null) {
            // Retrying cannot fix a malformed message, so send it straight to
            // the dead-letter queue.
            throw new AmqpRejectAndDontRequeueException("OrderCompletedEvent without an orderId");
        }
        log.info("Received OrderCompletedEvent for order {}", event.orderId());
        try {
            notificationService.sendOrderConfirmation(event);
        } catch (RuntimeException e) {
            log.error("Failed to process OrderCompletedEvent for order {}", event.orderId(), e);
            recordFailureQuietly(event.orderId(), e);
            throw e;
        }
    }

    private void recordFailureQuietly(Long orderId, RuntimeException cause) {
        try {
            notificationService.recordFailure(orderId, cause);
        } catch (RuntimeException e) {
            // The database itself may be the cause of the failure.
            log.warn("Could not record failed notification for order {}: {}", orderId, e.toString());
        }
    }
}
