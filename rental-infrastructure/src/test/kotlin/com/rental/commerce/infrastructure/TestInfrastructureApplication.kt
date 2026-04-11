package com.rental.commerce.infrastructure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan

@SpringBootApplication(
    scanBasePackages = ["com.rental.commerce.infrastructure"],
    exclude = [
        MongoAutoConfiguration::class,
        MongoDataAutoConfiguration::class,
        RedisAutoConfiguration::class,
        KafkaAutoConfiguration::class,
    ],
)
@EntityScan(basePackages = ["com.rental.commerce.domain"])
class TestInfrastructureApplication
