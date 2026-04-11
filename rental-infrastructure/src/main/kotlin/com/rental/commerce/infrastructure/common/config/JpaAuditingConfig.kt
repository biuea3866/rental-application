package com.rental.commerce.infrastructure.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.ZonedDateTime
import java.util.Optional

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "zonedDateTimeProvider")
class JpaAuditingConfig {

    @Bean
    fun zonedDateTimeProvider(): DateTimeProvider {
        return DateTimeProvider { Optional.of(ZonedDateTime.now()) }
    }
}
