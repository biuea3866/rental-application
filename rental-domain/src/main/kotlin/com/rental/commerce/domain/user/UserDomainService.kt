package com.rental.commerce.domain.user

import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ErrorCode
import org.springframework.stereotype.Service

@Service
class UserDomainService(
    private val userRepository: UserRepository,
) {

    fun checkEmailNotDuplicate(email: String) {
        if (userRepository.existsByEmail(email)) {
            throw DuplicateResourceException(errorCode = ErrorCode.DUPLICATE_EMAIL)
        }
    }
}
