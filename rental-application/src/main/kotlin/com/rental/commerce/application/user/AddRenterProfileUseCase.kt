package com.rental.commerce.application.user

import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddRenterProfileUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun execute(command: AddRenterProfileCommand): RenterProfileResponse {
        val savedProfile = userDomainService.saveRenterProfileAndGrantRole(
            userId = command.userId,
        )
        return RenterProfileResponse.from(savedProfile)
    }
}
