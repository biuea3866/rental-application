package com.rental.commerce.domain.user

interface UserRepository {

    fun save(user: User): User

    fun findById(userId: Long): User?

    fun findByEmail(email: String): User?

    fun findBySocialProviderAndSocialProviderId(
        socialProvider: SocialProvider,
        socialProviderId: String,
    ): User?

    fun existsByEmail(email: String): Boolean
}
