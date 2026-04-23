package com.rental.commerce.application.notification

import com.rental.commerce.domain.notification.NotificationPreference

data class NotificationPreferenceResult(
    val userId: Long,
    val chatEnabled: Boolean,
    val rentalEnabled: Boolean,
    val settlementEnabled: Boolean,
    val marketingEnabled: Boolean,
) {
    companion object {
        fun from(pref: NotificationPreference) = NotificationPreferenceResult(
            userId = pref.userId,
            chatEnabled = pref.chatEnabled,
            rentalEnabled = pref.rentalEnabled,
            settlementEnabled = pref.settlementEnabled,
            marketingEnabled = pref.marketingEnabled,
        )
    }
}
