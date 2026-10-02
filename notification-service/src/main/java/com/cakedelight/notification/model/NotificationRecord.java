package com.cakedelight.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An in-app order confirmation and its delivery status. Written only by the
 * RabbitMQ listener; the REST API exposes it read-only.
 *
 * orderId refers to an order in order-service's own database, so it is a
 * plain indexed column rather than a foreign key.
 */
@Entity
@Table(name = "notification_record", indexes = @Index(name = "idx_notification_order_id", columnList = "order_id"))
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationRecord() {
    }

    public NotificationRecord(Long orderId, String message, DeliveryStatus status) {
        this.orderId = orderId;
        this.message = message;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getMessage() { return message; }
    public DeliveryStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
