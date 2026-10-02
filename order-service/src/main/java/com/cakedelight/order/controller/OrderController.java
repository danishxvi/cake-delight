package com.cakedelight.order.controller;

import com.cakedelight.order.dto.AddItemRequest;
import com.cakedelight.order.dto.CreateOrderRequest;
import com.cakedelight.order.dto.UpdateQuantityRequest;
import com.cakedelight.order.model.Order;
import com.cakedelight.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Basket and order REST API, reached by clients through the API Gateway at
 * /api/orders. Errors are translated to HTTP responses by
 * {@link com.cakedelight.order.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createBasket(@Valid @RequestBody(required = false) CreateOrderRequest request) {
        Order created = orderService.createBasket(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/{id}/items")
    public Order addItem(@PathVariable Long id, @Valid @RequestBody AddItemRequest request) {
        return orderService.addItemToBasket(id, request);
    }

    @PutMapping("/{id}/items/{itemId}")
    public Order updateItemQuantity(@PathVariable Long id, @PathVariable Long itemId,
                                    @Valid @RequestBody UpdateQuantityRequest request) {
        return orderService.updateItemQuantity(id, itemId, request.quantity());
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public Order removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return orderService.removeItemFromBasket(id, itemId);
    }

    @PostMapping("/{id}/checkout")
    public Order checkout(@PathVariable Long id) {
        return orderService.checkout(id);
    }
}
