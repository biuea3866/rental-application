package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationPreferenceDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyNotificationPreferenceUseCase(
    private val preferenceDomainService: NotificationPreferenceDomainService,
) {
    fun execute(userId: Long): NotificationPreferenceResult =
        NotificationPreferenceResult.from(preferenceDomainService.getOrCreate(userId))
}
