package com.tharunch.orderfulfillment.event;

import com.tharunch.orderfulfillment.model.InventoryReservation;
import com.tharunch.orderfulfillment.repository.InventoryReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryReservationListenerTest {

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Captor
    private ArgumentCaptor<InventoryReservation> reservationCaptor;

    @Test
    void onOrderCreated_reservesInventoryForEachLineItem() {
        InventoryReservationListener listener = new InventoryReservationListener(reservationRepository);
        OrderCreatedEvent event = new OrderCreatedEvent(
                "order-123",
                "ada@example.com",
                new BigDecimal("25.50"),
                List.of(new OrderCreatedEvent.Item("SKU-1", 2), new OrderCreatedEvent.Item("SKU-2", 1)),
                Instant.now()
        );

        listener.onOrderCreated(event);

        verify(reservationRepository, times(2)).save(reservationCaptor.capture());
        List<InventoryReservation> reservations = reservationCaptor.getAllValues();

        assertThat(reservations).hasSize(2);
        assertThat(reservations.get(0).getOrderNumber()).isEqualTo("order-123");
        assertThat(reservations.get(0).getProductSku()).isEqualTo("SKU-1");
        assertThat(reservations.get(0).getQuantityReserved()).isEqualTo(2);
        assertThat(reservations.get(1).getProductSku()).isEqualTo("SKU-2");
        assertThat(reservations.get(1).getQuantityReserved()).isEqualTo(1);
    }
}
