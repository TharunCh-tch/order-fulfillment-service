package com.tharunch.orderfulfillment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Published to the {@code order.created} topic whenever a new order is
 * persisted. Consumed by {@link InventoryReservationListener} to reserve
 * stock for each line item.
 */
public record OrderCreatedEvent(
        String orderNumber,
        String customerEmail,
        BigDecimal totalAmount,
        List<Item> items,
        Instant occurredAt
) {
    public record Item(String productSku, int quantity) {
    }
}
