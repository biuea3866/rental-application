package com.rental.commerce.application.notification

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.notification.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkNotificationReadUseCase(
    private val notificationRepository: NotificationRepository,
) {

    @Transactional
    fun execute(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.NOTIFICATION_NOT_FOUND,
                message = "알림을 찾을 수 없습니다 (id=$notificationId)",
            )

        notification.markAsRead()
        notificationRepository.save(notification)
    }
}
