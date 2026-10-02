package com.cakedelight.order.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Message published to RabbitMQ when a checkout succeeds.
 *
 * notification-service keeps its own copy of this record. Messages are JSON,
 * so the two copies only need matching field names; any change here must be
 * mirrored there (see docs/EVENT-CONTRACT.md).
 */
public record OrderCompletedEvent(Long orderId, BigDecimal totalAmount, String status, Instant completedAt) {
}
