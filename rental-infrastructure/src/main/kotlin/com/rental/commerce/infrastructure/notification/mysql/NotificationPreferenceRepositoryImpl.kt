package com.rental.commerce.infrastructure.notification.mysql

import com.rental.commerce.domain.notification.NotificationPreference
import com.rental.commerce.domain.notification.NotificationPreferenceRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationPreferenceRepositoryImpl(
    private val jpaRepository: NotificationPreferenceJpaRepository,
) : NotificationPreferenceRepository {

    override fun save(preference: NotificationPreference): NotificationPreference =
        jpaRepository.save(preference)

    override fun findByUserId(userId: Long): NotificationPreference? =
        jpaRepository.findByUserId(userId)
}
