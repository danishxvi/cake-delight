package com.cakedelight.order.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A basket while PENDING, an order once COMPLETED. The table is named
 * customer_order because ORDER is a reserved SQL keyword.
 */
@Entity
@Table(name = "customer_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    // orphanRemoval deletes the row of an item removed from the basket instead
    // of leaving it behind with a null order_id.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean hasStatus(OrderStatus expected) {
        return status == expected;
    }

    public void markCompleted() {
        status = OrderStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public Optional<OrderItem> findItem(Long itemId) {
        return items.stream().filter(item -> item.getId() != null && item.getId().equals(itemId)).findFirst();
    }

    public Optional<OrderItem> findItemForCake(Long cakeId) {
        return items.stream().filter(item -> item.getCakeId().equals(cakeId)).findFirst();
    }

    /** Recomputes the total from the items; never trusted from the client. */
    public void recalculateTotal() {
        totalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<OrderItem> getItems() { return items; }
}
