package com.tharunch.orderfulfillment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(

        @NotBlank(message = "productSku is required")
        String productSku,

        @NotBlank(message = "productName is required")
        String productName,

        @Positive(message = "quantity must be greater than zero")
        int quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.01", message = "unitPrice must be greater than zero")
        BigDecimal unitPrice
) {
}
