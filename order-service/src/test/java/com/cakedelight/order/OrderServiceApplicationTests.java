package com.cakedelight.order;

import com.cakedelight.order.client.CakeSnapshot;
import com.cakedelight.order.client.CatalogClient;
import com.cakedelight.order.dto.AddItemRequest;
import com.cakedelight.order.dto.CreateOrderRequest;
import com.cakedelight.order.model.Order;
import com.cakedelight.order.service.OrderEventPublisher;
import com.cakedelight.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Boots the full application against an in-memory database and checks that a
 * basket round-trips through JPA, including the removal of an item.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private CatalogClient catalogClient;

    @MockitoBean
    private OrderEventPublisher eventPublisher;

    @Test
    void basketLifecyclePersistsCorrectly() {
        when(catalogClient.findCake(1L)).thenReturn(Optional.of(new CakeSnapshot(1L, "Chocolate Truffle Cake", new BigDecimal("799.00"), true)));
        when(catalogClient.findCake(2L)).thenReturn(Optional.of(new CakeSnapshot(2L, "Red Velvet Delight", new BigDecimal("849.00"), true)));

        Order basket = orderService.createBasket(new CreateOrderRequest(List.of(new AddItemRequest(1L, 2))));
        basket = orderService.addItemToBasket(basket.getId(), new AddItemRequest(2L, 1));
        assertThat(basket.getTotalAmount()).isEqualByComparingTo("2447.00");

        Long itemId = basket.getItems().get(0).getId();
        basket = orderService.removeItemFromBasket(basket.getId(), itemId);

        Order reloaded = orderService.getOrder(basket.getId());
        assertThat(reloaded.getItems()).extracting("cakeName").containsExactly("Red Velvet Delight");
        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo("849.00");
    }
}
