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
        sendSafely(ORDER_CREATED_TOPIC, event.orderNumber(), event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Publishing OrderStatusChangedEvent for order {}: {} -> {}",
                event.orderNumber(), event.previousStatus(), event.newStatus());
        sendSafely(ORDER_STATUS_CHANGED_TOPIC, event.orderNumber(), event);
    }

    /**
     * Publishing an order event is a side effect of the REST request, not a dependency of it: per
     * the architecture this notifies a decoupled downstream consumer, so a broker outage (or, e.g.,
     * running the app locally against H2 with no broker configured at all) must not fail the HTTP
     * request that triggered it. {@code KafkaTemplate.send()} can itself throw synchronously (e.g. a
     * metadata-fetch timeout) before it ever returns a future, so both the synchronous call and the
     * async completion are guarded here.
     */
    private void sendSafely(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event with key {} to topic {}", key, topic, ex);
                }
            });
        } catch (Exception ex) {
            log.error("Failed to publish event with key {} to topic {}", key, topic, ex);
        }
    }
}
