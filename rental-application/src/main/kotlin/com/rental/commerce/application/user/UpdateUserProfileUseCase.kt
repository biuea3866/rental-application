package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateUserProfileUseCase(
    private val userRepository: UserRepository,
) {

    @Transactional
    fun execute(command: UpdateUserProfileCommand) {
        val user = userRepository.findById(command.userId)
            ?: throw ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)

        user.updateProfile(
            name = command.name,
            phone = command.phone,
        )
    }
}
