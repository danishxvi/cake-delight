package com.cakedelight.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PUT /api/orders/{id}/items/{itemId}. Only the quantity can
 * change; the cake, name and price of an existing line are fixed.
 */
public record UpdateQuantityRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = OrderLimits.MAX_QUANTITY, message = "quantity must be at most " + OrderLimits.MAX_QUANTITY)
        Integer quantity) {
}
