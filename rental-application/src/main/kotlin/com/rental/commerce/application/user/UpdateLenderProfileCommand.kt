package com.rental.commerce.application.user

data class UpdateLenderProfileCommand(
    val userId: Long,
    val settlementAccountBank: String?,
    val settlementAccountNumber: String?,
)
