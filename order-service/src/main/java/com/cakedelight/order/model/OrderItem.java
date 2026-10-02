package com.cakedelight.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One line in a basket.
 *
 * cakeId refers to a cake in cake-catalog-service's own database, so it is a
 * plain column rather than a foreign key. The cake's name and price are copied
 * from the catalog when the item is added, so a completed order keeps the
 * price the customer actually paid even if the catalog changes later.
 */
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cakeId;

    @Column(nullable = false, length = 100)
    private String cakeName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    protected OrderItem() {
    }

    public OrderItem(Long cakeId, String cakeName, BigDecimal price, Integer quantity) {
        this.cakeId = cakeId;
        this.cakeName = cakeName;
        this.price = price;
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Long getCakeId() { return cakeId; }
    public String getCakeName() { return cakeName; }
    public BigDecimal getPrice() { return price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
