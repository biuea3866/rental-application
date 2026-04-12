package com.rental.commerce.domain.notification

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notification")
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    val notificationId: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    val notificationType: NotificationType,

    @Column(name = "reference_id")
    val referenceId: Long? = null,

    @Column(name = "reference_type", length = 30)
    val referenceType: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

) : BaseEntity() {

    fun markAsRead() {
        this.isRead = true
    }
}
