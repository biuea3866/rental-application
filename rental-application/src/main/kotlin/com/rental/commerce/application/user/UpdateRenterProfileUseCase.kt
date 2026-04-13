package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateRenterProfileUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun execute(command: UpdateRenterProfileCommand): RenterProfileResponse {
        val profile = userDomainService.findRenterProfileByUserId(command.userId)
            ?: throw ResourceNotFoundException(ErrorCode.RENTER_PROFILE_NOT_FOUND)

        profile.updateProfile(shippingAddress = command.shippingAddress)

        return RenterProfileResponse.from(profile)
    }
}
