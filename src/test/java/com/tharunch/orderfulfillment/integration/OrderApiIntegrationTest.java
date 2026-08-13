package com.tharunch.orderfulfillment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tharunch.orderfulfillment.dto.CreateOrderRequest;
import com.tharunch.orderfulfillment.dto.OrderItemRequest;
import com.tharunch.orderfulfillment.dto.UpdateOrderStatusRequest;
import com.tharunch.orderfulfillment.event.OrderEventProducer;
import com.tharunch.orderfulfillment.model.OrderStatus;
import com.tharunch.orderfulfillment.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full REST -&gt; service -&gt; JPA(H2) flow, exercised without Docker. The Kafka
 * producer is mocked here since no broker is available in this slice; the
 * real producer/consumer wiring against a live broker is covered by the
 * Testcontainers-tagged tests, which require Docker and run in CI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // See OrderControllerTest for why this is a plain instance rather than an
    // injected bean under Spring Boot 4's Jackson 3 auto-configuration.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderEventProducer orderEventProducer;

    @Test
    void fullOrderLifecycle_createGetUpdateStatusCancel() throws Exception {
        CreateOrderRequest createRequest = new CreateOrderRequest(
                "Grace Hopper",
                "grace@example.com",
                List.of(
                        new OrderItemRequest("SKU-100", "Compiler Manual", 1, new BigDecimal("42.00")),
                        new OrderItemRequest("SKU-200", "Debugging Kit", 3, new BigDecimal("9.99"))
                )
        );

        String createResponseJson = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(71.97))
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(createResponseJson).get("orderNumber").asText();
        assertThat(orderRepository.findByOrderNumber(orderNumber)).isPresent();

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Grace Hopper"))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.PAID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // SHIPPED is not reachable directly from PAID -> CANCELLED is invalid after PAID? No: PAID -> CANCELLED is allowed.
        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Once cancelled, further transitions are rejected.
        mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.PAID))))
                .andExpect(status().isConflict());
    }

    @Test
    void getOrder_returns404ForUnknownOrderNumber() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderNumber}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
