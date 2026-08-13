package com.tharunch.orderfulfillment.repository;

import com.tharunch.orderfulfillment.model.Order;
import com.tharunch.orderfulfillment.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);
}
