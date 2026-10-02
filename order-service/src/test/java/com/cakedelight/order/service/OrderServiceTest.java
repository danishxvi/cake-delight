package com.cakedelight.order.service;

import com.cakedelight.order.client.CakeSnapshot;
import com.cakedelight.order.client.CatalogClient;
import com.cakedelight.order.dto.AddItemRequest;
import com.cakedelight.order.exception.ConflictException;
import com.cakedelight.order.exception.ResourceNotFoundException;
import com.cakedelight.order.exception.ServiceUnavailableException;
import com.cakedelight.order.model.Order;
import com.cakedelight.order.model.OrderItem;
import com.cakedelight.order.model.OrderStatus;
import com.cakedelight.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final CakeSnapshot TRUFFLE = new CakeSnapshot(1L, "Chocolate Truffle Cake", new BigDecimal("799.00"), true);
    private static final CakeSnapshot WALNUT = new CakeSnapshot(8L, "Coffee Walnut Cake", new BigDecimal("799.00"), false);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Order basket;

    @BeforeEach
    void setUp() {
        basket = new Order();
        lenient().when(orderRepository.findById(10L)).thenReturn(Optional.of(basket));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addItemTakesNameAndPriceFromCatalog() {
        when(catalogClient.findCake(1L)).thenReturn(Optional.of(TRUFFLE));

        Order result = orderService.addItemToBasket(10L, new AddItemRequest(1L, 2));

        OrderItem item = result.getItems().get(0);
        assertThat(item.getCakeName()).isEqualTo("Chocolate Truffle Cake");
        assertThat(item.getPrice()).isEqualByComparingTo("799.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("1598.00");
    }

    @Test
    void addingSameCakeTwiceMergesIntoOneLine() {
        when(catalogClient.findCake(1L)).thenReturn(Optional.of(TRUFFLE));

        orderService.addItemToBasket(10L, new AddItemRequest(1L, 1));
        Order result = orderService.addItemToBasket(10L, new AddItemRequest(1L, 2));

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void addItemRejectsUnavailableCake() {
        when(catalogClient.findCake(8L)).thenReturn(Optional.of(WALNUT));

        assertThatThrownBy(() -> orderService.addItemToBasket(10L, new AddItemRequest(8L, 1)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void addItemRejectsUnknownCake() {
        when(catalogClient.findCake(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addItemToBasket(10L, new AddItemRequest(999L, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    void addItemReportsCatalogOutage() {
        when(catalogClient.findCake(1L)).thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> orderService.addItemToBasket(10L, new AddItemRequest(1L, 1)))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void addItemToMissingOrderIsNotFound() {
        when(orderRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addItemToBasket(77L, new AddItemRequest(1L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removingUnknownItemIsNotFound() {
        assertThatThrownBy(() -> orderService.removeItemFromBasket(10L, 12345L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void checkoutOfEmptyBasketIsRejected() {
        assertThatThrownBy(() -> orderService.checkout(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("empty");
        verify(eventPublisher, never()).publishOrderCompletion(any());
    }

    @Test
    void checkoutCompletesOrderAndPublishesEvent() {
        when(catalogClient.findCake(1L)).thenReturn(Optional.of(TRUFFLE));
        orderService.addItemToBasket(10L, new AddItemRequest(1L, 1));

        Order result = orderService.checkout(10L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(eventPublisher).publishOrderCompletion(result);
    }

    @Test
    void completedOrderCannotBeCheckedOutOrChangedAgain() {
        when(catalogClient.findCake(1L)).thenReturn(Optional.of(TRUFFLE));
        orderService.addItemToBasket(10L, new AddItemRequest(1L, 1));
        orderService.checkout(10L);

        assertThatThrownBy(() -> orderService.checkout(10L)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> orderService.addItemToBasket(10L, new AddItemRequest(1L, 1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void checkoutReportsBrokerOutage() {
        when(catalogClient.findCake(1L)).thenReturn(Optional.of(TRUFFLE));
        orderService.addItemToBasket(10L, new AddItemRequest(1L, 1));
        doThrow(new AmqpConnectException(new ConnectException("connection refused")))
                .when(eventPublisher).publishOrderCompletion(any());

        assertThatThrownBy(() -> orderService.checkout(10L))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
