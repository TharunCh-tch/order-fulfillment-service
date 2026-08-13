package com.tharunch.orderfulfillment.event;

import com.tharunch.orderfulfillment.model.OrderStatus;

import java.time.Instant;

/**
 * Published to the {@code order.status-changed} topic whenever an order
 * transitions state (e.g. PAID, SHIPPED, CANCELLED). Downstream services
 * (notifications, shipping, analytics) would subscribe to this topic in a
 * real deployment; within this single-service demo it is produced but not
 * additionally consumed.
 */
public record OrderStatusChangedEvent(
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt
) {
}
