package com.rental.commerce.infrastructure.user

import com.rental.commerce.domain.user.SocialProvider
import com.rental.commerce.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?

    fun findBySocialProviderAndSocialProviderId(
        socialProvider: SocialProvider,
        socialProviderId: String,
    ): User?

    fun existsByEmail(email: String): Boolean
}
