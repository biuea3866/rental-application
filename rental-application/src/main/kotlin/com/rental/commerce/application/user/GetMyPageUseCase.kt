package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.LenderProfileRepository
import com.rental.commerce.domain.user.RenterProfileRepository
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMyPageUseCase(
    private val userRepository: UserRepository,
    private val lenderProfileRepository: LenderProfileRepository,
    private val renterProfileRepository: RenterProfileRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): MyPageResponse {
        val user = userRepository.findById(userId)
            ?: throw ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)

        val lenderProfile = lenderProfileRepository.findByUserId(userId)
        val renterProfile = renterProfileRepository.findByUserId(userId)

        return MyPageResponse.from(
            user = user,
            lenderProfile = lenderProfile,
            renterProfile = renterProfile,
        )
    }
}
