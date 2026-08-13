package com.tharunch.orderfulfillment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    public static final String ORDER_CREATED_TOPIC = "order.created";
    public static final String ORDER_STATUS_CHANGED_TOPIC = "order.status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order {}", event.orderNumber());
        kafkaTemplate.send(ORDER_CREATED_TOPIC, event.orderNumber(), event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Publishing OrderStatusChangedEvent for order {}: {} -> {}",
                event.orderNumber(), event.previousStatus(), event.newStatus());
        kafkaTemplate.send(ORDER_STATUS_CHANGED_TOPIC, event.orderNumber(), event);
    }
}
