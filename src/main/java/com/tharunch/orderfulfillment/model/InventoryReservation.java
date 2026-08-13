package com.tharunch.orderfulfillment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Written by {@link com.tharunch.orderfulfillment.event.InventoryReservationListener}
 * when it consumes an {@code order.created} Kafka event. Represents stock held
 * against a given order's line items, simulating a downstream inventory service
 * reacting to an upstream order-service event.
 */
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 36)
    private String orderNumber;

    @Column(name = "product_sku", nullable = false, length = 64)
    private String productSku;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    protected InventoryReservation() {
        // JPA
    }

    public InventoryReservation(String orderNumber, String productSku, int quantityReserved) {
        this.orderNumber = orderNumber;
        this.productSku = productSku;
        this.quantityReserved = quantityReserved;
        this.reservedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getProductSku() {
        return productSku;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }
}
