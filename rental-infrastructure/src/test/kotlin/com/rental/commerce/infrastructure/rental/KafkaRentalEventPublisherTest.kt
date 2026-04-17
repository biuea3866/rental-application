package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.infrastructure.kafka.RentalTopics
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.ZonedDateTime

@SpringBootTest(
    classes = [KafkaRentalEventPublisherTestConfig::class],
    webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("test")
class KafkaRentalEventPublisherTest : BehaviorSpec() {

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

        private fun buildConsumerProps(bootstrapServers: String): Map<String, Any> = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-kafka-publisher-group",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "*",
            JsonDeserializer.VALUE_DEFAULT_TYPE to RentalStatusChangedEvent::class.java.name,
        )

        private fun buildProducerProps(bootstrapServers: String): Map<String, Any> = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        )
    }

    init {
        given("KafkaRentalEventPublisher — RentalStatusChangedEvent 발행") {

            `when`("유효한 RentalStatusChangedEvent를 publish() 호출하면") {
                val bootstrapServers = kafkaContainer.bootstrapServers
                val producerProps = buildProducerProps(bootstrapServers)
                val kafkaTemplate = KafkaTemplate(
                    DefaultKafkaProducerFactory<String, RentalStatusChangedEvent>(producerProps)
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

                then("토픽이 RentalTopics.STATUS_CHANGED로 발행된다") {
                    val consumerProps = buildConsumerProps(bootstrapServers)
                    val consumerFactory = DefaultKafkaConsumerFactory<String, RentalStatusChangedEvent>(consumerProps)
                    val consumer = consumerFactory.createConsumer()
                    consumer.subscribe(listOf(RentalTopics.STATUS_CHANGED))

                    publisher.publish(event)

                    val records = consumer.poll(Duration.ofSeconds(5))
                    consumer.close()

                    records.count() shouldBe 1
                    val received = records.first().value()
                    received.rentalId shouldBe 1L
                    received.fromStatus shouldBe RentalStatus.REQUESTED
                    received.toStatus shouldBe RentalStatus.APPROVED
                }
            }

            `when`("publishAll() 로 여러 이벤트를 발행하면") {
                val bootstrapServers = kafkaContainer.bootstrapServers
                val producerProps = buildProducerProps(bootstrapServers)
                val kafkaTemplate = KafkaTemplate(
                    DefaultKafkaProducerFactory<String, RentalStatusChangedEvent>(producerProps)
                )
                val publisher = KafkaRentalEventPublisher(kafkaTemplate)

                val events = listOf(
                    RentalStatusChangedEvent(
                        rentalId = 2L,
                        renterId = 11L,
                        lenderId = 21L,
                        fromStatus = RentalStatus.REQUESTED,
                        toStatus = RentalStatus.APPROVED,
                        occurredAt = ZonedDateTime.now(),
                    ),
                    RentalStatusChangedEvent(
                        rentalId = 2L,
                        renterId = 11L,
                        lenderId = 21L,
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
    }
}
