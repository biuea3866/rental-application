package com.rental.commerce.infrastructure.refund

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan

/**
 * RefundEventChainE2ETest 전용 Spring 컨텍스트.
 *
 * infrastructure 와 domain 패키지를 모두 스캔하여
 * - DisputeResolvedRefundListener 가 의존하는 RefundDomainService
 * - RefundCompletedSettlementListener 가 의존하는 SettlementDomainService
 * - RentalStatusChangedEventWorker 가 의존하는 NotificationDomainService
 * 등 도메인 서비스 빈이 정상 등록되도록 한다.
 *
 * Kafka/Redis/Mongo/MinIO 는 E2E 범위에 불필요하므로 exclude.
 * allow-bean-definition-overriding 은 application.properties 로 설정.
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.rental.commerce.infrastructure",
        "com.rental.commerce.domain",
    ],
    exclude = [
        MongoAutoConfiguration::class,
        MongoDataAutoConfiguration::class,
        RedisAutoConfiguration::class,
        RedisRepositoriesAutoConfiguration::class,
        KafkaAutoConfiguration::class,
    ],
)
@EntityScan(basePackages = ["com.rental.commerce.domain"])
class RefundE2ETestApplication
