package com.rental.commerce.domain.notification

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.ResourceNotFoundException
import org.springframework.stereotype.Service

@Service
class NotificationDomainService(
    private val notificationRepository: NotificationRepository,
) {

    fun getNotifications(userId: Long, pageQuery: PageQuery): PageResult<Notification> {
        return notificationRepository.findByUserId(userId, pageQuery)
    }

    fun getNotificationOrThrow(notificationId: Long): Notification {
        return notificationRepository.findById(notificationId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.NOTIFICATION_NOT_FOUND,
                message = "알림을 찾을 수 없습니다 (id=$notificationId)",
            )
    }

    fun save(notification: Notification): Notification {
        return notificationRepository.save(notification)
    }

    fun markAllAsRead(userId: Long) {
        notificationRepository.markAllAsReadByUserId(userId)
    }

    fun countUnread(userId: Long): Long {
        return notificationRepository.countUnreadByUserId(userId)
    }
}
