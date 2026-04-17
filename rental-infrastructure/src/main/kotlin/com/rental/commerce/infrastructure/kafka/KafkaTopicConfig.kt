package com.rental.commerce.infrastructure.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    @Bean
    fun rentalStatusChangedTopic(): NewTopic =
        TopicBuilder.name(RentalTopics.STATUS_CHANGED)
            .partitions(3)
            .replicas(1)
            .build()
}
