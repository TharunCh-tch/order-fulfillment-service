package com.tharunch.orderfulfillment;

import org.springframework.boot.SpringApplication;

public class TestOrderFulfillmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderFulfillmentServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
