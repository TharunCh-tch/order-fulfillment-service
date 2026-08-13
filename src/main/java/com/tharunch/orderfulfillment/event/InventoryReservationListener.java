package com.tharunch.orderfulfillment.event;

import com.tharunch.orderfulfillment.model.InventoryReservation;
import com.tharunch.orderfulfillment.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to {@code order.created} events by reserving inventory for each
 * line item of the new order. In a real system this logic would live in a
 * separate inventory microservice; it is colocated here to demonstrate a
 * complete produce -&gt; consume -&gt; persist flow within one deployable
 * unit for portfolio purposes.
 */
@Component
public class InventoryReservationListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationListener.class);

    private final InventoryReservationRepository reservationRepository;

    public InventoryReservationListener(InventoryReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @KafkaListener(topics = OrderEventProducer.ORDER_CREATED_TOPIC, groupId = "inventory-reservation-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order {}, reserving {} line item(s)",
                event.orderNumber(), event.items().size());

        for (OrderCreatedEvent.Item item : event.items()) {
            InventoryReservation reservation =
                    new InventoryReservation(event.orderNumber(), item.productSku(), item.quantity());
            reservationRepository.save(reservation);
        }
    }
}
