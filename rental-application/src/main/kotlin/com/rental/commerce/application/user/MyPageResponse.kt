package com.rental.commerce.application.user

import com.rental.commerce.domain.user.LenderProfile
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.User

data class MyPageResponse(
    val userId: Long,
    val email: String,
    val name: String,
    val phone: String,
    val role: String,
    val lenderProfile: LenderProfileResponse?,
    val renterProfile: RenterProfileResponse?,
) {
    companion object {
        fun from(
            user: User,
            lenderProfile: LenderProfile?,
            renterProfile: RenterProfile?,
        ): MyPageResponse = MyPageResponse(
            userId = requireNotNull(user.userId) { "persisted User must have a non-null PK" },
            email = user.email,
            name = user.name,
            phone = user.phone,
            role = user.role.name,
            lenderProfile = lenderProfile?.let { LenderProfileResponse.from(it) },
            renterProfile = renterProfile?.let { RenterProfileResponse.from(it) },
        )
    }
}
