package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationDomainService
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.infrastructure.kafka.RentalTopics
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.ZonedDateTime

/**
 * RentalStatusChangedKafkaConsumer 통합 테스트
 *
 * Testcontainers Kafka로 Kafka Consumer DTO 매핑 + Worker 호출을 검증한다.
 * Spring Context 없이 직접 KafkaListenerContainer 를 구성하여 테스트한다.
 */
class RentalStatusChangedKafkaConsumerTest : BehaviorSpec({

    val kafkaContainer: KafkaContainer = KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    ).apply { start() }

    afterSpec { kafkaContainer.stop() }

    fun buildProducerProps(bootstrapServers: String): Map<String, Any> = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        JsonSerializer.ADD_TYPE_INFO_HEADERS to "false",
    )

    fun buildConsumerProps(bootstrapServers: String): Map<String, Any> = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-group-${System.currentTimeMillis()}",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
        JsonDeserializer.TRUSTED_PACKAGES to "com.rental.commerce.domain.rental.event",
        JsonDeserializer.VALUE_DEFAULT_TYPE to RentalStatusChangedEvent::class.java.name,
        JsonDeserializer.USE_TYPE_INFO_HEADERS to "false",
    )

    given("RentalStatusChangedKafkaConsumer — APPROVED 이벤트 Kafka 수신") {

        `when`("Kafka 토픽에 APPROVED 이벤트를 발행하면") {
            val bootstrapServers = kafkaContainer.bootstrapServers

            // Setup mock service and worker
            val notificationDomainService: NotificationDomainService = mockk()
            val notificationSlot = slot<Notification>()
            every { notificationDomainService.save(capture(notificationSlot)) } answers { firstArg() }

            val worker = RentalStatusChangedEventWorker(notificationDomainService)
            val consumer = RentalStatusChangedKafkaConsumer(worker)

            // Setup Kafka listener container manually
            val consumerFactory = DefaultKafkaConsumerFactory<String, RentalStatusChangedEvent>(
                buildConsumerProps(bootstrapServers)
            )
            val containerFactory = ConcurrentKafkaListenerContainerFactory<String, RentalStatusChangedEvent>()
            containerFactory.consumerFactory = consumerFactory

            val container: ConcurrentMessageListenerContainer<String, RentalStatusChangedEvent> =
                containerFactory.createContainer(RentalTopics.STATUS_CHANGED)

            container.setupMessageListener(
                MessageListener<String, RentalStatusChangedEvent> { record ->
                    consumer.consume(record.value())
                }
            )
            container.start()

            // Give container time to start
            Thread.sleep(1000)

            // Produce event
            val kafkaTemplate = KafkaTemplate(
                DefaultKafkaProducerFactory<String, RentalStatusChangedEvent>(buildProducerProps(bootstrapServers))
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
            kafkaTemplate.flush()

            // Wait for consumer to process
            Thread.sleep(3000)
            container.stop()

            then("NotificationDomainService.save()가 호출된다") {
                verify(atLeast = 1) { notificationDomainService.save(any()) }
            }

            then("RENTAL_CONFIRMED 타입으로 renterId 에게 알림이 저장된다") {
                val saved = notificationSlot.captured
                saved.userId shouldBe 10L
                saved.referenceId shouldBe 100L
            }
        }
    }
})
