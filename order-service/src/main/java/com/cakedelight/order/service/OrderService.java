package com.cakedelight.order.service;

import com.cakedelight.order.client.CakeSnapshot;
import com.cakedelight.order.client.CatalogClient;
import com.cakedelight.order.dto.AddItemRequest;
import com.cakedelight.order.dto.CreateOrderRequest;
import com.cakedelight.order.dto.OrderLimits;
import com.cakedelight.order.exception.ConflictException;
import com.cakedelight.order.exception.ResourceNotFoundException;
import com.cakedelight.order.exception.ServiceUnavailableException;
import com.cakedelight.order.model.Order;
import com.cakedelight.order.model.OrderItem;
import com.cakedelight.order.model.OrderStatus;
import com.cakedelight.order.repository.OrderRepository;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Basket and order operations. A basket is a PENDING order that can be edited
 * freely; checkout makes it COMPLETED and publishes an event for
 * notification-service.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient,
                        OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.eventPublisher = eventPublisher;
    }

    public Order createBasket(CreateOrderRequest request) {
        Order order = new Order();
        if (request != null && request.items() != null) {
            request.items().forEach(item -> addOrMergeItem(order, item));
        }
        order.recalculateTotal();
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return findOrder(orderId);
    }

    public Order addItemToBasket(Long orderId, AddItemRequest request) {
        Order order = findPendingOrder(orderId);
        addOrMergeItem(order, request);
        order.recalculateTotal();
        return orderRepository.save(order);
    }

    public Order updateItemQuantity(Long orderId, Long itemId, int quantity) {
        Order order = findPendingOrder(orderId);
        OrderItem item = order.findItem(itemId)
                .orElseThrow(() -> itemNotFound(orderId, itemId));
        item.setQuantity(quantity);
        order.recalculateTotal();
        return orderRepository.save(order);
    }

    public Order removeItemFromBasket(Long orderId, Long itemId) {
        Order order = findPendingOrder(orderId);
        OrderItem item = order.findItem(itemId)
                .orElseThrow(() -> itemNotFound(orderId, itemId));
        order.getItems().remove(item);
        order.recalculateTotal();
        return orderRepository.save(order);
    }

    public Order checkout(Long orderId) {
        Order order = findOrder(orderId);
        if (order.hasStatus(OrderStatus.COMPLETED)) {
            throw new ConflictException("Order " + orderId + " is already completed");
        }
        if (order.getItems().isEmpty()) {
            throw new ConflictException("Cannot check out an empty basket");
        }
        order.markCompleted();
        Order saved = orderRepository.saveAndFlush(order);
        // Published after the order is flushed, so the event never describes an
        // order that failed to save. If publishing fails, the exception rolls the
        // transaction back and the client can safely retry the checkout.
        try {
            eventPublisher.publishOrderCompletion(saved);
        } catch (AmqpException e) {
            throw new ServiceUnavailableException(
                    "The order could not be completed right now. Please try again shortly.", e);
        }
        return saved;
    }

    private void addOrMergeItem(Order order, AddItemRequest request) {
        CakeSnapshot cake = fetchCake(request.cakeId());
        if (!cake.isAvailable()) {
            throw new ConflictException(cake.name() + " is currently unavailable");
        }
        Optional<OrderItem> existing = order.findItemForCake(cake.id());
        if (existing.isPresent()) {
            OrderItem item = existing.get();
            int newQuantity = item.getQuantity() + request.quantity();
            if (newQuantity > OrderLimits.MAX_QUANTITY) {
                throw new IllegalArgumentException("quantity must be at most " + OrderLimits.MAX_QUANTITY);
            }
            item.setQuantity(newQuantity);
        } else {
            order.getItems().add(new OrderItem(cake.id(), cake.name(), cake.price(), request.quantity()));
        }
    }

    private CakeSnapshot fetchCake(Long cakeId) {
        Optional<CakeSnapshot> cake;
        try {
            cake = catalogClient.findCake(cakeId);
        } catch (RestClientException | IllegalStateException e) {
            throw new ServiceUnavailableException(
                    "The catalog service is unavailable right now. Please try again shortly.", e);
        }
        return cake.orElseThrow(() -> new IllegalArgumentException("Cake with id " + cakeId + " does not exist"));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " was not found"));
    }

    private Order findPendingOrder(Long orderId) {
        Order order = findOrder(orderId);
        if (!order.hasStatus(OrderStatus.PENDING)) {
            throw new ConflictException("Order " + orderId + " is already completed and can no longer be changed");
        }
        return order;
    }

    private static ResourceNotFoundException itemNotFound(Long orderId, Long itemId) {
        return new ResourceNotFoundException("Item " + itemId + " was not found in order " + orderId);
    }
}
