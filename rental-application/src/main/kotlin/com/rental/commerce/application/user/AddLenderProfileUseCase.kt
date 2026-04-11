package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.LenderProfile
import com.rental.commerce.domain.user.LenderProfileRepository
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddLenderProfileUseCase(
    private val userRepository: UserRepository,
    private val lenderProfileRepository: LenderProfileRepository,
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun execute(command: AddLenderProfileCommand): LenderProfileResponse {
        val user = userRepository.findById(command.userId)
            ?: throw ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)

        userDomainService.checkLenderProfileNotDuplicate(command.userId)

        val profile = LenderProfile(
            userId = command.userId,
            lenderType = command.lenderType,
        )

        val savedProfile = lenderProfileRepository.save(profile)

        user.addRole(UserRole.LENDER)
        userRepository.save(user)

        return LenderProfileResponse.from(savedProfile)
    }
}
