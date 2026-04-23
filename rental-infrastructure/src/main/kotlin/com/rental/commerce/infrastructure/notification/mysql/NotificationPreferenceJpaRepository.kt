package com.rental.commerce.infrastructure.notification.mysql

import com.rental.commerce.domain.notification.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceJpaRepository : JpaRepository<NotificationPreference, Long> {
    fun findByUserId(userId: Long): NotificationPreference?
}
