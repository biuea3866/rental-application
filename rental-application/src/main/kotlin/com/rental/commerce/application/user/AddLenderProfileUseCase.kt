package com.rental.commerce.application.user

import com.rental.commerce.domain.user.UserDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddLenderProfileUseCase(
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun execute(command: AddLenderProfileCommand): LenderProfileResponse {
        val savedProfile = userDomainService.saveLenderProfileAndGrantRole(
            userId = command.userId,
            lenderType = command.lenderType,
        )
        return LenderProfileResponse.from(savedProfile)
    }
}
