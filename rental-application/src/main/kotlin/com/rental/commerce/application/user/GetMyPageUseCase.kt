package com.rental.commerce.application.user

import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMyPageUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): MyPageResponse {
        val user = userDomainService.findById(userId)
        val lenderProfile = userDomainService.findLenderProfileByUserId(userId)
        val renterProfile = userDomainService.findRenterProfileByUserId(userId)

        return MyPageResponse.from(
            user = user,
            lenderProfile = lenderProfile,
            renterProfile = renterProfile,
        )
    }
}
