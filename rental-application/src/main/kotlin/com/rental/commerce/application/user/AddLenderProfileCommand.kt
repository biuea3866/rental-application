package com.rental.commerce.application.user

import com.rental.commerce.domain.user.LenderType

data class AddLenderProfileCommand(
    val userId: Long,
    val lenderType: LenderType = LenderType.INDIVIDUAL,
)
