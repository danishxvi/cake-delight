package com.cakedelight.order.service;

import com.cakedelight.order.config.RabbitMQConfig;
import com.cakedelight.order.event.OrderCompletedEvent;
import com.cakedelight.order.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order events to RabbitMQ, keeping messaging details out of
 * {@link OrderService}.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCompletion(Order order) {
        OrderCompletedEvent event = new OrderCompletedEvent(
                order.getId(), order.getTotalAmount(), order.getStatus().name(), order.getCompletedAt());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);
        log.info("Published OrderCompletedEvent for order {}", order.getId());
    }
}
