package com.rental.commerce.application.user

import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateUserProfileUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun execute(command: UpdateUserProfileCommand) {
        val user = userDomainService.findById(command.userId)

        user.updateProfile(
            name = command.name,
            phone = command.phone,
        )
    }
}
