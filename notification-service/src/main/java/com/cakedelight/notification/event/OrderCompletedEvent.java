package com.cakedelight.notification.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Message received from RabbitMQ when a checkout succeeds.
 *
 * This is this service's own copy of order-service's event. Messages are
 * JSON, so the two copies only need matching field names; any change in
 * order-service must be mirrored here (see docs/EVENT-CONTRACT.md).
 */
public record OrderCompletedEvent(Long orderId, BigDecimal totalAmount, String status, Instant completedAt) {
}
