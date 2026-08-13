package com.tharunch.orderfulfillment.dto;

import com.tharunch.orderfulfillment.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "status is required")
        OrderStatus status
) {
}
