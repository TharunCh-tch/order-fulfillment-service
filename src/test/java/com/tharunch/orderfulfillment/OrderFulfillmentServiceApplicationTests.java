package com.tharunch.orderfulfillment;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Boots the full application context against real Testcontainers-backed
 * Postgres and Kafka. Requires Docker; excluded from the default test task.
 */
@Tag("testcontainers")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderFulfillmentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
