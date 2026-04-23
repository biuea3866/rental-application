package com.rental.commerce.application.notification

data class UpdateNotificationPreferenceCommand(
    val userId: Long,
    val chatEnabled: Boolean? = null,
    val rentalEnabled: Boolean? = null,
    val settlementEnabled: Boolean? = null,
    val marketingEnabled: Boolean? = null,
)
