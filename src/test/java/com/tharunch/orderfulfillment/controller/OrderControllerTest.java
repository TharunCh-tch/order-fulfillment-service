package com.tharunch.orderfulfillment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tharunch.orderfulfillment.dto.CreateOrderRequest;
import com.tharunch.orderfulfillment.dto.OrderItemRequest;
import com.tharunch.orderfulfillment.dto.UpdateOrderStatusRequest;
import com.tharunch.orderfulfillment.exception.InvalidOrderStateException;
import com.tharunch.orderfulfillment.exception.OrderNotFoundException;
import com.tharunch.orderfulfillment.model.Order;
import com.tharunch.orderfulfillment.model.OrderItem;
import com.tharunch.orderfulfillment.model.OrderStatus;
import com.tharunch.orderfulfillment.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Instantiated directly rather than injected: Spring Boot 4's auto-configured
    // ObjectMapper bean is the new Jackson 3 (tools.jackson) type, while this
    // classic com.fasterxml.jackson ObjectMapper is only used here to build
    // request bodies for the test - it does not need to be a Spring bean.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    private Order sampleOrder() {
        Order order = new Order("order-123", "Ada Lovelace", "ada@example.com");
        order.addItem(new OrderItem("SKU-1", "Widget", 2, new BigDecimal("10.00")));
        return order;
    }

    @Test
    void createOrder_returns201WithLocationHeader() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Ada Lovelace", "ada@example.com",
                List.of(new OrderItemRequest("SKU-1", "Widget", 2, new BigDecimal("10.00"))));
        when(orderService.createOrder(any())).thenReturn(sampleOrder());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("order-123"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(20.00));
    }

    @Test
    void createOrder_rejectsInvalidPayloadWith400() throws Exception {
        String invalidJson = """
                {"customerName": "", "customerEmail": "not-an-email", "items": []}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void getOrder_returns200WhenFound() throws Exception {
        when(orderService.getOrder("order-123")).thenReturn(sampleOrder());

        mockMvc.perform(get("/api/v1/orders/order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Ada Lovelace"));
    }

    @Test
    void getOrder_returns404WhenMissing() throws Exception {
        when(orderService.getOrder("missing")).thenThrow(new OrderNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/orders/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listOrders_returnsPagedResults() throws Exception {
        Page<Order> page = new PageImpl<>(List.of(sampleOrder()), PageRequest.of(0, 20), 1);
        when(orderService.listOrders(eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("order-123"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateStatus_returnsUpdatedOrder() throws Exception {
        Order order = sampleOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderService.updateStatus(eq("order-123"), eq(OrderStatus.PAID))).thenReturn(order);

        mockMvc.perform(patch("/api/v1/orders/order-123/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.PAID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void updateStatus_returns409OnInvalidTransition() throws Exception {
        when(orderService.updateStatus(anyString(), any()))
                .thenThrow(new InvalidOrderStateException("Cannot transition order order-123 from DELIVERED to CANCELLED"));

        mockMvc.perform(patch("/api/v1/orders/order-123/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CANCELLED))))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOrder_returnsCancelledOrder() throws Exception {
        Order order = sampleOrder();
        order.setStatus(OrderStatus.CANCELLED);
        when(orderService.cancelOrder("order-123")).thenReturn(order);

        mockMvc.perform(post("/api/v1/orders/order-123/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void deleteOrder_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/order-123"))
                .andExpect(status().isNoContent());
    }
}
