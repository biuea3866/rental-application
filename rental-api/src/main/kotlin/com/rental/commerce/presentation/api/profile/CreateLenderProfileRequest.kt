package com.rental.commerce.presentation.api.profile

import com.rental.commerce.application.user.AddLenderProfileCommand
import com.rental.commerce.domain.user.LenderType
import jakarta.validation.constraints.NotBlank

data class CreateLenderProfileRequest(
    @field:NotBlank(message = "대여자 유형은 필수입니다")
    val lenderType: String,
) {
    fun toCommand(userId: Long): AddLenderProfileCommand = AddLenderProfileCommand(
        userId = userId,
        lenderType = LenderType.valueOf(lenderType),
    )
}
