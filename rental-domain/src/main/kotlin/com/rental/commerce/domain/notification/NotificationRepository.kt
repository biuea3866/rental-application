package com.rental.commerce.domain.notification

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface NotificationRepository {

    fun save(notification: Notification): Notification

    fun findByUserId(userId: Long, pageable: Pageable): Page<Notification>

    fun findById(notificationId: Long): Notification?

    fun countUnreadByUserId(userId: Long): Long

    fun markAllAsReadByUserId(userId: Long)
}
