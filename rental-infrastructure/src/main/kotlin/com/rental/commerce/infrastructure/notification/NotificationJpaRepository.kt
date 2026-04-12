package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.notification.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationJpaRepository : JpaRepository<Notification, Long>
