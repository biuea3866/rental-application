package com.rental.commerce.infrastructure.kafka

import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConsumerConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
) {

    @Bean
    fun rentalStatusChangedConsumerFactory(): ConsumerFactory<String, RentalStatusChangedEvent> {
        val props: Map<String, Any> = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.rental.commerce.domain.rental.event",
            JsonDeserializer.VALUE_DEFAULT_TYPE to RentalStatusChangedEvent::class.java.name,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun rentalStatusChangedKafkaListenerContainerFactory():
        ConcurrentKafkaListenerContainerFactory<String, RentalStatusChangedEvent> {
        return ConcurrentKafkaListenerContainerFactory<String, RentalStatusChangedEvent>().apply {
            consumerFactory = rentalStatusChangedConsumerFactory()
            setConcurrency(3)
        }
    }
}
