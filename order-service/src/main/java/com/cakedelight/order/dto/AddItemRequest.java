package com.cakedelight.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for adding a cake to a basket. The name and price are looked
 * up from the catalog, so the client only sends the cake id and a quantity.
 */
public record AddItemRequest(
        @NotNull(message = "cakeId is required")
        Long cakeId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = OrderLimits.MAX_QUANTITY, message = "quantity must be at most " + OrderLimits.MAX_QUANTITY)
        Integer quantity) {
}
