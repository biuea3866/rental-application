package com.rental.commerce.domain.notification

interface NotificationPreferenceRepository {

    fun save(preference: NotificationPreference): NotificationPreference

    fun findByUserId(userId: Long): NotificationPreference?
}
