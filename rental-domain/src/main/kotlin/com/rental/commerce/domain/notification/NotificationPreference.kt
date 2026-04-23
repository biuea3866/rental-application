package com.rental.commerce.domain.notification

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * NotificationPreference (PRD-004 §4.5, V23).
 *
 * 사용자별 채널 on/off 설정. UNIQUE(user_id) — 1:1.
 *
 * 기본값:
 *  chat/rental/settlement = ON, marketing = OFF
 */
@Entity
@Table(
    name = "notification_preference",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id"])],
)
class NotificationPreference private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    chatEnabled: Boolean = true,
    rentalEnabled: Boolean = true,
    settlementEnabled: Boolean = true,
    marketingEnabled: Boolean = false,

) : BaseEntity() {

    @Column(name = "chat_enabled", nullable = false)
    var chatEnabled: Boolean = chatEnabled
        protected set

    @Column(name = "rental_enabled", nullable = false)
    var rentalEnabled: Boolean = rentalEnabled
        protected set

    @Column(name = "settlement_enabled", nullable = false)
    var settlementEnabled: Boolean = settlementEnabled
        protected set

    @Column(name = "marketing_enabled", nullable = false)
    var marketingEnabled: Boolean = marketingEnabled
        protected set

    fun isEnabled(channel: NotificationChannel): Boolean = when (channel) {
        NotificationChannel.CHAT -> chatEnabled
        NotificationChannel.RENTAL -> rentalEnabled
        NotificationChannel.SETTLEMENT -> settlementEnabled
        NotificationChannel.MARKETING -> marketingEnabled
    }

    fun update(
        chatEnabled: Boolean? = null,
        rentalEnabled: Boolean? = null,
        settlementEnabled: Boolean? = null,
        marketingEnabled: Boolean? = null,
    ) {
        chatEnabled?.let { this.chatEnabled = it }
        rentalEnabled?.let { this.rentalEnabled = it }
        settlementEnabled?.let { this.settlementEnabled = it }
        marketingEnabled?.let { this.marketingEnabled = it }
    }

    companion object {
        fun createDefault(userId: Long) = NotificationPreference(userId = userId)
    }
}
