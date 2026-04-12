package com.rental.commerce.domain.notification

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "notification")
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    val id: Long = 0L,

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

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = null,

) : BaseEntity() {

    fun markAsRead() {
        this.isRead = true
    }

    fun verifyOwner(ownerId: Long) {
        if (this.userId != ownerId) throw BusinessException(ErrorCode.FORBIDDEN)
    }
}
