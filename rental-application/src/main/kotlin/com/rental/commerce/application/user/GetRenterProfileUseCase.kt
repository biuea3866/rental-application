package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetRenterProfileUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): RenterProfileResponse {
        val profile = userDomainService.findRenterProfileByUserId(userId)
            ?: throw ResourceNotFoundException(ErrorCode.RENTER_PROFILE_NOT_FOUND)

        return RenterProfileResponse.from(profile)
    }
}
