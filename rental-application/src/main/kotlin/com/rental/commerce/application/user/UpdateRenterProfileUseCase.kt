package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.RenterProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateRenterProfileUseCase(
    private val renterProfileRepository: RenterProfileRepository,
) {

    @Transactional
    fun execute(command: UpdateRenterProfileCommand): RenterProfileResponse {
        val profile = renterProfileRepository.findByUserId(command.userId)
            ?: throw ResourceNotFoundException(ErrorCode.RENTER_PROFILE_NOT_FOUND)

        profile.updateProfile(shippingAddress = command.shippingAddress)

        return RenterProfileResponse.from(profile)
    }
}
