package com.rental.commerce.application.user

import com.rental.commerce.domain.user.RenterProfile

data class RenterProfileResponse(
    val renterProfileId: Long,
    val userId: Long,
    val trustGrade: String,
    val totalTransactionCount: Int,
) {
    companion object {
        fun from(entity: RenterProfile): RenterProfileResponse = RenterProfileResponse(
            renterProfileId = entity.renterProfileId,
            userId = entity.userId,
            trustGrade = entity.trustGrade.name,
            totalTransactionCount = entity.totalTransactionCount,
        )
    }
}
