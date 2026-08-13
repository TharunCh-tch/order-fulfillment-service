package com.tharunch.orderfulfillment.service;

import com.tharunch.orderfulfillment.dto.CreateOrderRequest;
import com.tharunch.orderfulfillment.dto.OrderItemRequest;
import com.tharunch.orderfulfillment.event.OrderCreatedEvent;
import com.tharunch.orderfulfillment.event.OrderEventProducer;
import com.tharunch.orderfulfillment.event.OrderStatusChangedEvent;
import com.tharunch.orderfulfillment.exception.InvalidOrderStateException;
import com.tharunch.orderfulfillment.exception.OrderNotFoundException;
import com.tharunch.orderfulfillment.model.Order;
import com.tharunch.orderfulfillment.model.OrderItem;
import com.tharunch.orderfulfillment.model.OrderStatus;
import com.tharunch.orderfulfillment.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /** Allowed forward transitions for each order status. Anything not listed here is terminal. */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    public OrderServiceImpl(OrderRepository orderRepository, OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String orderNumber = UUID.randomUUID().toString();
        Order order = new Order(orderNumber, request.customerName(), request.customerEmail());

        for (OrderItemRequest itemRequest : request.items()) {
            order.addItem(new OrderItem(
                    itemRequest.productSku(),
                    itemRequest.productName(),
                    itemRequest.quantity(),
                    itemRequest.unitPrice()
            ));
        }

        Order saved = orderRepository.save(order);
        log.info("Created order {} with {} item(s), total={}", saved.getOrderNumber(), saved.getItems().size(), saved.getTotalAmount());

        orderEventProducer.publishOrderCreated(toOrderCreatedEvent(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(String orderNumber) {
        return findOrThrow(orderNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> listOrders(OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByStatus(status, pageable);
        }
        return orderRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Order updateStatus(String orderNumber, OrderStatus newStatus) {
        Order order = findOrThrow(orderNumber);
        transition(order, newStatus);
        return order;
    }

    @Override
    @Transactional
    public Order cancelOrder(String orderNumber) {
        Order order = findOrThrow(orderNumber);
        transition(order, OrderStatus.CANCELLED);
        return order;
    }

    @Override
    @Transactional
    public void deleteOrder(String orderNumber) {
        Order order = findOrThrow(orderNumber);
        orderRepository.delete(order);
        log.info("Deleted order {}", orderNumber);
    }

    private void transition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStateException(
                    "Cannot transition order %s from %s to %s".formatted(order.getOrderNumber(), currentStatus, newStatus));
        }

        order.setStatus(newStatus);
        log.info("Order {} transitioned {} -> {}", order.getOrderNumber(), currentStatus, newStatus);

        orderEventProducer.publishOrderStatusChanged(
                new OrderStatusChangedEvent(order.getOrderNumber(), currentStatus, newStatus, Instant.now()));
    }

    private Order findOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    private OrderCreatedEvent toOrderCreatedEvent(Order order) {
        var items = order.getItems().stream()
                .map(item -> new OrderCreatedEvent.Item(item.getProductSku(), item.getQuantity()))
                .toList();
        return new OrderCreatedEvent(order.getOrderNumber(), order.getCustomerEmail(), order.getTotalAmount(), items, Instant.now());
    }
}
