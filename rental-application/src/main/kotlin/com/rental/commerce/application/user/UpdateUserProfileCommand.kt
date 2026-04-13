package com.rental.commerce.application.user

data class UpdateUserProfileCommand(
    val userId: Long,
    val name: String?,
    val phone: String?,
)
