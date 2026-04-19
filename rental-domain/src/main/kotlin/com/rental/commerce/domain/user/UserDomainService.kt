package com.rental.commerce.domain.user

import com.rental.commerce.domain.common.DuplicateResourceException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.common.SocialUserInfo
import org.springframework.stereotype.Service

@Service
class UserDomainService(
    private val userRepository: UserRepository,
    private val lenderProfileRepository: LenderProfileRepository,
    private val renterProfileRepository: RenterProfileRepository,
) {

    fun findById(userId: Long): User =
        userRepository.findById(userId)
            ?: throw ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)

    fun findByEmail(email: String): User =
        userRepository.findByEmail(email)
            ?: throw ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)

    fun saveUser(user: User): User =
        userRepository.save(user)

    fun findBySocialProviderAndSocialProviderId(
        provider: SocialProvider,
        socialProviderId: String,
    ): User? = userRepository.findBySocialProviderAndSocialProviderId(provider, socialProviderId)

    fun findOrCreateSocialUser(socialUserInfo: SocialUserInfo): User {
        val existingBySocial = userRepository.findBySocialProviderAndSocialProviderId(
            socialUserInfo.provider,
            socialUserInfo.socialId,
        )
        if (existingBySocial != null) {
            return existingBySocial
        }

        val existingByEmail = userRepository.findByEmail(socialUserInfo.email)
        if (existingByEmail != null) {
            existingByEmail.linkSocialAccount(socialUserInfo.provider, socialUserInfo.socialId)
            return userRepository.save(existingByEmail)
        }

        val newUser = User.registerSocial(
            email = socialUserInfo.email,
            name = socialUserInfo.name,
            socialProvider = socialUserInfo.provider,
            socialProviderId = socialUserInfo.socialId,
        )
        return userRepository.save(newUser)
    }

    fun saveLenderProfileAndGrantRole(userId: Long, lenderType: LenderType): LenderProfile {
        val user = findById(userId)
        checkLenderProfileNotDuplicate(userId)

        val profile = LenderProfile(
            userId = userId,
            lenderType = lenderType,
        )
        val savedProfile = lenderProfileRepository.save(profile)

        user.addRole(UserRole.LENDER)
        userRepository.save(user)

        return savedProfile
    }

    fun saveRenterProfileAndGrantRole(userId: Long): RenterProfile {
        val user = findById(userId)
        checkRenterProfileNotDuplicate(userId)

        val profile = RenterProfile(userId = userId)
        val savedProfile = renterProfileRepository.save(profile)

        user.addRole(UserRole.RENTER)
        userRepository.save(user)

        return savedProfile
    }

    fun findLenderProfileByUserId(userId: Long): LenderProfile? =
        lenderProfileRepository.findByUserId(userId)

    fun findRenterProfileByUserId(userId: Long): RenterProfile? =
        renterProfileRepository.findByUserId(userId)

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
