package com.cakedelight.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the order-completion event. Identical to
 * order-service's RabbitMQConfig, so this service does not depend on
 * order-service having started first. Keep the two in sync.
 *
 * A message that still fails after the listener's retries is moved to the
 * dead-letter queue instead of being redelivered forever.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "order_exchange";
    public static final String QUEUE_NAME = "order_completion_queue";
    public static final String ROUTING_KEY = "order.completed";

    public static final String DEAD_LETTER_EXCHANGE_NAME = "order_exchange.dlx";
    public static final String DEAD_LETTER_QUEUE_NAME = "order_completion_queue.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "order.completed.dead";

    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE_NAME)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE_NAME).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    // The JSON body is mapped onto this service's own OrderCompletedEvent,
    // inferred from the @RabbitListener method parameter.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
