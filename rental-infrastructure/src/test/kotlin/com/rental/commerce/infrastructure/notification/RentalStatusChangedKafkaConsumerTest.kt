package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationDomainService
import com.rental.commerce.domain.notification.NotificationType
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.infrastructure.kafka.RentalTopics
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.ZonedDateTime

@SpringBootTest(
    classes = [RentalStatusChangedKafkaConsumerTestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("test")
class RentalStatusChangedKafkaConsumerTest : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    companion object {
        val kafkaContainer: KafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
        ).apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
        }

        private fun buildProducerProps(bootstrapServers: String): Map<String, Any> = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        )
    }

    init {
        given("RentalStatusChangedKafkaConsumer — APPROVED 이벤트 Kafka 수신") {

            `when`("Kafka 토픽에 APPROVED 이벤트를 발행하면") {
                val bootstrapServers = kafkaContainer.bootstrapServers
                val producerProps = buildProducerProps(bootstrapServers)
                val kafkaTemplate = KafkaTemplate(
                    DefaultKafkaProducerFactory<String, RentalStatusChangedEvent>(producerProps)
                )

                val event = RentalStatusChangedEvent(
                    rentalId = 100L,
                    renterId = 10L,
                    lenderId = 20L,
                    fromStatus = RentalStatus.REQUESTED,
                    toStatus = RentalStatus.APPROVED,
                    occurredAt = ZonedDateTime.now(),
                )

                kafkaTemplate.send(RentalTopics.STATUS_CHANGED, event.rentalId.toString(), event).get()

                // Kafka consumer listener async processing
                Thread.sleep(3000)

                then("NotificationDomainService.save()가 호출된다") {
                    // Verified by MockBean in test config
                    verify(atLeast = 1) {
                        RentalStatusChangedKafkaConsumerTestConfig.mockNotificationService.save(any())
                    }
                }
            }
        }
    }
}

@Configuration
@Import(
    RentalStatusChangedKafkaConsumer::class,
    RentalStatusChangedEventWorker::class,
)
class RentalStatusChangedKafkaConsumerTestConfig {

    companion object {
        val mockNotificationService: NotificationDomainService = mockk {
            every { save(any()) } answers { firstArg<Notification>() }
        }
    }

    @Bean
    fun notificationDomainService(): NotificationDomainService = mockNotificationService
}
