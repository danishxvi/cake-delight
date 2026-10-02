package com.cakedelight.order.repository;

import com.cakedelight.order.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Loads the items in the same query, so the order can be serialized after
    // the transaction has closed (open-in-view is disabled).
    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(Long id);
}
