package com.cakedelight.order.controller;

import com.cakedelight.order.exception.ConflictException;
import com.cakedelight.order.exception.ResourceNotFoundException;
import com.cakedelight.order.exception.ServiceUnavailableException;
import com.cakedelight.order.model.Order;
import com.cakedelight.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createBasketWithoutBodyReturns201() throws Exception {
        when(orderService.createBasket(any())).thenReturn(new Order());

        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        when(orderService.getOrder(5L)).thenThrow(new ResourceNotFoundException("Order 5 was not found"));

        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order 5 was not found"));
    }

    @Test
    void changingCompletedOrderReturns409() throws Exception {
        when(orderService.addItemToBasket(eq(5L), any()))
                .thenThrow(new ConflictException("Order 5 is already completed and can no longer be changed"));

        mockMvc.perform(post("/api/orders/5/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cakeId\":1,\"quantity\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void catalogOutageReturns503() throws Exception {
        when(orderService.addItemToBasket(eq(5L), any()))
                .thenThrow(new ServiceUnavailableException("The catalog service is unavailable right now.", null));

        mockMvc.perform(post("/api/orders/5/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cakeId\":1,\"quantity\":1}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void zeroQuantityIsRejected() throws Exception {
        mockMvc.perform(put("/api/orders/5/items/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("quantity: quantity must be at least 1"));
    }

    @Test
    void invalidNestedItemIsRejected() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cakeId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }
}
