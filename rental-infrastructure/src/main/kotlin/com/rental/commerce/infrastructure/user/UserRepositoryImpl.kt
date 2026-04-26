package com.rental.commerce.infrastructure.user

import com.rental.commerce.domain.user.QUser
import com.rental.commerce.domain.common.SocialProvider
import com.rental.commerce.domain.user.User
import com.rental.commerce.domain.user.UserRepository
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : UserRepository {

    private val user = QUser.user

    override fun save(user: User): User {
        return userJpaRepository.save(user)
    }

    override fun findById(userId: Long): User? {
        return queryFactory
            .selectFrom(user)
            .where(user.id.eq(userId))
            .fetchOne()
    }

    override fun findByEmail(email: String): User? {
        return queryFactory
            .selectFrom(user)
            .where(user.email.eq(email))
            .fetchOne()
    }

    override fun findBySocialProviderAndSocialProviderId(
        socialProvider: SocialProvider,
        socialProviderId: String,
    ): User? {
        return queryFactory
            .selectFrom(user)
            .where(
                user.socialProvider.eq(socialProvider),
                user.socialProviderId.eq(socialProviderId),
            )
            .fetchOne()
    }

    override fun existsByEmail(email: String): Boolean {
        return queryFactory
            .selectOne()
            .from(user)
            .where(user.email.eq(email))
            .fetchFirst() != null
    }
}
