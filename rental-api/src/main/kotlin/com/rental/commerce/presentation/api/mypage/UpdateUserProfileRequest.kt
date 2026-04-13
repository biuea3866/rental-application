package com.rental.commerce.presentation.api.mypage

import com.rental.commerce.application.user.UpdateUserProfileCommand

data class UpdateUserProfileRequest(
    val name: String?,
    val phone: String?,
) {
    fun toCommand(userId: Long): UpdateUserProfileCommand = UpdateUserProfileCommand(
        userId = userId,
        name = name,
        phone = phone,
    )
}
