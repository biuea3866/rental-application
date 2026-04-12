package com.rental.commerce.presentation.api.mypage

import com.rental.commerce.application.user.UpdateLenderProfileCommand

data class UpdateLenderProfileRequest(
    val settlementAccountBank: String?,
    val settlementAccountNumber: String?,
) {
    fun toCommand(userId: Long): UpdateLenderProfileCommand = UpdateLenderProfileCommand(
        userId = userId,
        settlementAccountBank = settlementAccountBank,
        settlementAccountNumber = settlementAccountNumber,
    )
}
