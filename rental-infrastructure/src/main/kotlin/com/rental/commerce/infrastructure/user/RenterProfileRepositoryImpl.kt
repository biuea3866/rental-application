package com.rental.commerce.infrastructure.user

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.user.QRenterProfile
import com.rental.commerce.domain.user.RenterProfile
import com.rental.commerce.domain.user.RenterProfileRepository
import org.springframework.stereotype.Repository

@Repository
class RenterProfileRepositoryImpl(
    private val renterProfileJpaRepository: RenterProfileJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : RenterProfileRepository {

    private val renterProfile = QRenterProfile.renterProfile

    override fun save(profile: RenterProfile): RenterProfile {
        return renterProfileJpaRepository.save(profile)
    }

    override fun findByUserId(userId: Long): RenterProfile? {
        return queryFactory
            .selectFrom(renterProfile)
            .where(renterProfile.userId.eq(userId))
            .fetchOne()
    }

    override fun existsByUserId(userId: Long): Boolean {
        return queryFactory
            .selectOne()
            .from(renterProfile)
            .where(renterProfile.userId.eq(userId))
            .fetchFirst() != null
    }
}
