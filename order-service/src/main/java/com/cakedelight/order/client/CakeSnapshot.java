package com.cakedelight.order.client;

import java.math.BigDecimal;

/**
 * The subset of cake-catalog-service's Cake resource that order-service needs.
 */
public record CakeSnapshot(Long id, String name, BigDecimal price, Boolean availability) {

    public boolean isAvailable() {
        return Boolean.TRUE.equals(availability);
    }
}
