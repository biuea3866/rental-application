package com.rental.commerce.application.user

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.RenterProfileRepository
import com.rental.commerce.domain.user.UserDomainService
import com.rental.commerce.domain.user.UserRepository
import com.rental.commerce.domain.user.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddRenterProfileUseCase(
    private val userRepository: UserRepository,
    private val renterProfileRepository: RenterProfileRepository,
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun execute(command: AddRenterProfileCommand): RenterProfileResponse {
        val user = userRepository.findById(command.userId)
            ?: throw ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)

        userDomainService.checkRenterProfileNotDuplicate(command.userId)

        val profile = RenterProfile(userId = command.userId)

        val savedProfile = renterProfileRepository.save(profile)

        user.addRole(UserRole.RENTER)
        userRepository.save(user)

        return RenterProfileResponse.from(savedProfile)
    }
}
