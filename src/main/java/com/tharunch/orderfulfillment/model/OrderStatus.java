package com.tharunch.orderfulfillment.model;

/**
 * Lifecycle states of an {@link Order}.
 * <p>
 * Valid transitions are enforced in the service layer:
 * CREATED -&gt; PAID -&gt; SHIPPED -&gt; DELIVERED, with CANCELLED reachable
 * from CREATED or PAID only.
 */
public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
