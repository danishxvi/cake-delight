package com.cakedelight.order.model;

/**
 * Lifecycle of an order. A PENDING order is an editable basket; COMPLETED is
 * final.
 */
public enum OrderStatus {
    PENDING,
    COMPLETED
}
