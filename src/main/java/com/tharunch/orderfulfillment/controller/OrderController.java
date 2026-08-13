package com.tharunch.orderfulfillment.controller;

import com.tharunch.orderfulfillment.dto.CreateOrderRequest;
import com.tharunch.orderfulfillment.dto.OrderResponse;
import com.tharunch.orderfulfillment.dto.UpdateOrderStatusRequest;
import com.tharunch.orderfulfillment.model.Order;
import com.tharunch.orderfulfillment.model.OrderStatus;
import com.tharunch.orderfulfillment.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        Order order = orderService.createOrder(request);
        var location = uriBuilder.path("/api/v1/orders/{orderNumber}").build(order.getOrderNumber());
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        Order order = orderService.getOrder(orderNumber);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OrderResponse> page = orderService.listOrders(status, pageable).map(OrderResponse::from);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable String orderNumber,
                                                        @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order order = orderService.updateStatus(orderNumber, request.status());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderNumber) {
        Order order = orderService.cancelOrder(orderNumber);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @DeleteMapping("/{orderNumber}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderNumber) {
        orderService.deleteOrder(orderNumber);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
