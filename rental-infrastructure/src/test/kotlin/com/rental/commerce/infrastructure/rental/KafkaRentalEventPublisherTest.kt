package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.common.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.infrastructure.kafka.RentalTopics
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.ZonedDateTime

/**
 * KafkaRentalEventPublisher 통합 테스트
 *
 * Testcontainers Kafka로 실제 발행/수신을 검증한다.
 * Spring Context 없이 직접 KafkaTemplate 을 구성하여 테스트한다.
 */
class KafkaRentalEventPublisherTest : BehaviorSpec({

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

    fun buildConsumerProps(bootstrapServers: String, groupId: String = "test-kafka-publisher-group-${System.nanoTime()}"): Map<String, Any> = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to groupId,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
        JsonDeserializer.TRUSTED_PACKAGES to "*",
        JsonDeserializer.VALUE_DEFAULT_TYPE to RentalStatusChangedEvent::class.java.name,
        JsonDeserializer.USE_TYPE_INFO_HEADERS to "false",
    )

    given("KafkaRentalEventPublisher — RentalStatusChangedEvent 발행") {

        `when`("유효한 RentalStatusChangedEvent를 publish() 호출하면") {
            val bootstrapServers = kafkaContainer.bootstrapServers
            val kafkaTemplate = KafkaTemplate(
                DefaultKafkaProducerFactory<String, RentalStatusChangedEvent>(buildProducerProps(bootstrapServers))
            )
            val publisher = KafkaRentalEventPublisher(kafkaTemplate)

            val event = RentalStatusChangedEvent(
                rentalId = 1L,
                renterId = 10L,
                lenderId = 20L,
                fromStatus = RentalStatus.REQUESTED,
                toStatus = RentalStatus.APPROVED,
                occurredAt = ZonedDateTime.now(),
            )

            then("예외 없이 발행에 성공한다") {
                shouldNotThrowAny {
                    publisher.publish(event)
                }
            }
        }

        `when`("RentalStatusChangedEvent를 publish 후 consumer로 수신하면") {
            val bootstrapServers = kafkaContainer.bootstrapServers
            val kafkaTemplate = KafkaTemplate(
                DefaultKafkaProducerFactory<String, RentalStatusChangedEvent>(buildProducerProps(bootstrapServers))
            )
            val publisher = KafkaRentalEventPublisher(kafkaTemplate)

            val event = RentalStatusChangedEvent(
                rentalId = 2L,
                renterId = 11L,
                lenderId = 21L,
                fromStatus = RentalStatus.REQUESTED,
                toStatus = RentalStatus.APPROVED,
                occurredAt = ZonedDateTime.now(),
            )

            val consumerFactory = DefaultKafkaConsumerFactory<String, RentalStatusChangedEvent>(
                buildConsumerProps(bootstrapServers)
            )
            val consumer = consumerFactory.createConsumer()
            consumer.subscribe(listOf(RentalTopics.STATUS_CHANGED))

            publisher.publish(event)
            kafkaTemplate.flush()

            val records = consumer.poll(Duration.ofSeconds(10))
            consumer.close()

            then("토픽이 RentalTopics.STATUS_CHANGED 로 발행되고 올바른 데이터를 수신한다") {
                val matchingRecords = records.map { it.value() }.filter { it.rentalId == 2L }
                matchingRecords.size shouldBe 1
                val received = matchingRecords.first()
                received.rentalId shouldBe 2L
                received.fromStatus shouldBe RentalStatus.REQUESTED
                received.toStatus shouldBe RentalStatus.APPROVED
            }
        }

        `when`("publishAll() 로 여러 이벤트를 발행하면") {
            val bootstrapServers = kafkaContainer.bootstrapServers
            val kafkaTemplate = KafkaTemplate(
                DefaultKafkaProducerFactory<String, RentalStatusChangedEvent>(buildProducerProps(bootstrapServers))
            )
            val publisher = KafkaRentalEventPublisher(kafkaTemplate)

            val events = listOf(
                RentalStatusChangedEvent(
                    rentalId = 3L,
                    renterId = 12L,
                    lenderId = 22L,
                    fromStatus = RentalStatus.REQUESTED,
                    toStatus = RentalStatus.APPROVED,
                    occurredAt = ZonedDateTime.now(),
                ),
                RentalStatusChangedEvent(
                    rentalId = 3L,
                    renterId = 12L,
                    lenderId = 22L,
                    fromStatus = RentalStatus.APPROVED,
                    toStatus = RentalStatus.PAID,
                    occurredAt = ZonedDateTime.now(),
                ),
            )

            then("예외 없이 모든 이벤트가 발행된다") {
                shouldNotThrowAny {
                    publisher.publishAll(events)
                }
            }
        }
    }
})
