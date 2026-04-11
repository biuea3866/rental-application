package com.rental.commerce.application.user

import com.rental.commerce.domain.user.LenderProfile

data class LenderProfileResponse(
    val lenderProfileId: Long,
    val userId: Long,
    val lenderType: String,
    val verificationStatus: String,
    val settlementAccountBank: String?,
    val settlementAccountNumber: String?,
) {
    companion object {
        fun from(entity: LenderProfile): LenderProfileResponse = LenderProfileResponse(
            lenderProfileId = entity.lenderProfileId,
            userId = entity.userId,
            lenderType = entity.lenderType.name,
            verificationStatus = entity.verificationStatus.name,
            settlementAccountBank = entity.settlementAccountBank,
            settlementAccountNumber = entity.settlementAccountNumber,
        )
    }
}
