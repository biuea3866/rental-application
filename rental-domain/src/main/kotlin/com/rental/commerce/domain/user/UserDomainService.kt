package com.rental.commerce.domain.user

import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ErrorCode
import org.springframework.stereotype.Service

@Service
class UserDomainService(
    private val userRepository: UserRepository,
    private val lenderProfileRepository: LenderProfileRepository,
    private val renterProfileRepository: RenterProfileRepository,
) {

    fun checkEmailNotDuplicate(email: String) {
        if (userRepository.existsByEmail(email)) {
            throw DuplicateResourceException(errorCode = ErrorCode.DUPLICATE_EMAIL)
        }
    }

    fun checkLenderProfileNotDuplicate(userId: Long) {
        if (lenderProfileRepository.existsByUserId(userId)) {
            throw DuplicateResourceException(
                errorCode = ErrorCode.LENDER_PROFILE_ALREADY_EXISTS,
            )
        }
    }

    fun checkRenterProfileNotDuplicate(userId: Long) {
        if (renterProfileRepository.existsByUserId(userId)) {
            throw DuplicateResourceException(
                errorCode = ErrorCode.RENTER_PROFILE_ALREADY_EXISTS,
            )
        }
    }
}
