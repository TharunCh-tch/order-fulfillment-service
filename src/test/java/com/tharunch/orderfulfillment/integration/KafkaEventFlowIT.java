package com.tharunch.orderfulfillment.integration;

import com.tharunch.orderfulfillment.TestcontainersConfiguration;
import com.tharunch.orderfulfillment.dto.CreateOrderRequest;
import com.tharunch.orderfulfillment.dto.OrderItemRequest;
import com.tharunch.orderfulfillment.model.InventoryReservation;
import com.tharunch.orderfulfillment.repository.InventoryReservationRepository;
import com.tharunch.orderfulfillment.service.OrderService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end verification of the real produce -&gt; broker -&gt; consume -&gt;
 * persist flow against an actual Kafka broker and PostgreSQL instance,
 * provisioned via Testcontainers ({@link TestcontainersConfiguration}).
 * <p>
 * Requires a running Docker daemon. Excluded from the default {@code test}
 * task (see build.gradle) and run via {@code ./gradlew integrationTest}
 * (GitHub Actions runs this task; it was not run in the local sandbox this
 * project was authored in, since Docker Desktop's engine was not available
 * there).
 */
@Tag("testcontainers")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class KafkaEventFlowIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void creatingAnOrder_triggersRealConsumerToReserveInventory() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Katherine Johnson",
                "katherine@example.com",
                List.of(new OrderItemRequest("SKU-900", "Slide Rule", 4, new BigDecimal("15.00")))
        );

        var order = orderService.createOrder(request);

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<InventoryReservation> reservations = reservationRepository.findByOrderNumber(order.getOrderNumber());
                    assertThat(reservations).hasSize(1);
                    assertThat(reservations.get(0).getProductSku()).isEqualTo("SKU-900");
                    assertThat(reservations.get(0).getQuantityReserved()).isEqualTo(4);
                });
    }
}
