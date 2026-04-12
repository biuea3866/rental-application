package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.LenderProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetLenderProfileUseCase(
    private val lenderProfileRepository: LenderProfileRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): LenderProfileResponse {
        val profile = lenderProfileRepository.findByUserId(userId)
            ?: throw ResourceNotFoundException(ErrorCode.LENDER_PROFILE_NOT_FOUND)

        return LenderProfileResponse.from(profile)
    }
}
