package com.tharunch.orderfulfillment.service;

import com.tharunch.orderfulfillment.dto.CreateOrderRequest;
import com.tharunch.orderfulfillment.model.Order;
import com.tharunch.orderfulfillment.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    Order createOrder(CreateOrderRequest request);

    Order getOrder(String orderNumber);

    Page<Order> listOrders(OrderStatus status, Pageable pageable);

    Order updateStatus(String orderNumber, OrderStatus newStatus);

    Order cancelOrder(String orderNumber);

    void deleteOrder(String orderNumber);
}
