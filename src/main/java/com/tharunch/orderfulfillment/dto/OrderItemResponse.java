package com.tharunch.orderfulfillment.dto;

import com.tharunch.orderfulfillment.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productSku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.lineTotal()
        );
    }
}
