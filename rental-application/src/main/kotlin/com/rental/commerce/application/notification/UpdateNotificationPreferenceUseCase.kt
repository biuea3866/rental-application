package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationPreferenceDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UpdateNotificationPreferenceUseCase(
    private val preferenceDomainService: NotificationPreferenceDomainService,
) {
    fun execute(command: UpdateNotificationPreferenceCommand): NotificationPreferenceResult {
        val updated = preferenceDomainService.update(
            userId = command.userId,
            chatEnabled = command.chatEnabled,
            rentalEnabled = command.rentalEnabled,
            settlementEnabled = command.settlementEnabled,
            marketingEnabled = command.marketingEnabled,
        )
        return NotificationPreferenceResult.from(updated)
    }
}
