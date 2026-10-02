package com.cakedelight.order.dto;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Optional request body for creating a basket, with any initial items.
 */
public record CreateOrderRequest(List<@Valid AddItemRequest> items) {
}
