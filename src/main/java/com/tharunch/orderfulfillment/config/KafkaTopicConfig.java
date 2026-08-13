package com.tharunch.orderfulfillment.config;

import com.tharunch.orderfulfillment.event.OrderEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(OrderEventProducer.ORDER_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(OrderEventProducer.ORDER_STATUS_CHANGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
