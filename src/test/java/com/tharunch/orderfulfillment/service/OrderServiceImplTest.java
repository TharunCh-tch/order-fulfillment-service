package com.tharunch.orderfulfillment.service;

import com.tharunch.orderfulfillment.dto.CreateOrderRequest;
import com.tharunch.orderfulfillment.dto.OrderItemRequest;
import com.tharunch.orderfulfillment.event.OrderCreatedEvent;
import com.tharunch.orderfulfillment.event.OrderEventProducer;
import com.tharunch.orderfulfillment.event.OrderStatusChangedEvent;
import com.tharunch.orderfulfillment.exception.InvalidOrderStateException;
import com.tharunch.orderfulfillment.exception.OrderNotFoundException;
import com.tharunch.orderfulfillment.model.Order;
import com.tharunch.orderfulfillment.model.OrderStatus;
import com.tharunch.orderfulfillment.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> orderCreatedEventCaptor;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, orderEventProducer);
    }

    @Test
    void createOrder_persistsOrderWithComputedTotalAndPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Ada Lovelace",
                "ada@example.com",
                List.of(
                        new OrderItemRequest("SKU-1", "Widget", 2, new BigDecimal("10.00")),
                        new OrderItemRequest("SKU-2", "Gadget", 1, new BigDecimal("5.50"))
                )
        );
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(request);

        assertThat(result.getCustomerName()).isEqualTo("Ada Lovelace");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("25.50");
        assertThat(result.getItems()).hasSize(2);

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderNumber()).isNotBlank();

        verify(orderEventProducer).publishOrderCreated(orderCreatedEventCaptor.capture());
        OrderCreatedEvent publishedEvent = orderCreatedEventCaptor.getValue();
        assertThat(publishedEvent.orderNumber()).isEqualTo(result.getOrderNumber());
        assertThat(publishedEvent.items()).hasSize(2);
        assertThat(publishedEvent.totalAmount()).isEqualByComparingTo("25.50");
    }

    @Test
    void getOrder_throwsWhenOrderMissing() {
        when(orderRepository.findByOrderNumber("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder("missing"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void updateStatus_allowsValidTransitionAndPublishesEvent() {
        Order order = new Order("order-123", "Ada Lovelace", "ada@example.com");
        when(orderRepository.findByOrderNumber("order-123")).thenReturn(Optional.of(order));

        Order result = orderService.updateStatus("order-123", OrderStatus.PAID);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderEventProducer).publishOrderStatusChanged(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateStatus_rejectsInvalidTransition() {
        Order order = new Order("order-123", "Ada Lovelace", "ada@example.com");
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus("order-123", OrderStatus.CANCELLED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("DELIVERED");

        verify(orderEventProducer, never()).publishOrderStatusChanged(any());
    }

    @Test
    void cancelOrder_allowedFromCreated() {
        Order order = new Order("order-123", "Ada Lovelace", "ada@example.com");
        when(orderRepository.findByOrderNumber("order-123")).thenReturn(Optional.of(order));

        Order result = orderService.cancelOrder("order-123");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_rejectedWhenAlreadyShipped() {
        Order order = new Order("order-123", "Ada Lovelace", "ada@example.com");
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByOrderNumber("order-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("order-123"))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void deleteOrder_removesExistingOrder() {
        Order order = new Order("order-123", "Ada Lovelace", "ada@example.com");
        when(orderRepository.findByOrderNumber("order-123")).thenReturn(Optional.of(order));

        orderService.deleteOrder("order-123");

        verify(orderRepository, times(1)).delete(order);
    }
}
