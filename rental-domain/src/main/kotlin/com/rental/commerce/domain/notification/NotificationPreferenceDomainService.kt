package com.rental.commerce.domain.notification

import org.springframework.stereotype.Service

/**
 * NotificationPreferenceDomainService.
 *
 * - getOrCreate: 누락 시 기본값으로 생성 후 반환 (신규 가입자 접근 시)
 * - update: 부분 업데이트 (null 인 필드는 유지)
 */
@Service
class NotificationPreferenceDomainService(
    private val preferenceRepository: NotificationPreferenceRepository,
) {
    fun getOrCreate(userId: Long): NotificationPreference =
        preferenceRepository.findByUserId(userId)
            ?: preferenceRepository.save(NotificationPreference.createDefault(userId))

    fun update(
        userId: Long,
        chatEnabled: Boolean? = null,
        rentalEnabled: Boolean? = null,
        settlementEnabled: Boolean? = null,
        marketingEnabled: Boolean? = null,
    ): NotificationPreference {
        val preference = getOrCreate(userId)
        preference.update(chatEnabled, rentalEnabled, settlementEnabled, marketingEnabled)
        return preferenceRepository.save(preference)
    }
}
