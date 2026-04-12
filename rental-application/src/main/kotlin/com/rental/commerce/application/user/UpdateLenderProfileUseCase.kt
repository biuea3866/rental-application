package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateLenderProfileUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun execute(command: UpdateLenderProfileCommand): LenderProfileResponse {
        val profile = userDomainService.findLenderProfileByUserId(command.userId)
            ?: throw ResourceNotFoundException(ErrorCode.LENDER_PROFILE_NOT_FOUND)

        profile.updateProfile(
            settlementAccountBank = command.settlementAccountBank,
            settlementAccountNumber = command.settlementAccountNumber,
        )

        return LenderProfileResponse.from(profile)
    }
}
