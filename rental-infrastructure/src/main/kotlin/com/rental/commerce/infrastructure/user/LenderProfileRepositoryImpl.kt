package com.rental.commerce.infrastructure.user

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.user.LenderProfile
import com.rental.commerce.domain.user.LenderProfileRepository
import com.rental.commerce.domain.user.QLenderProfile
import org.springframework.stereotype.Repository

@Repository
class LenderProfileRepositoryImpl(
    private val lenderProfileJpaRepository: LenderProfileJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : LenderProfileRepository {

    private val lenderProfile = QLenderProfile.lenderProfile

    override fun save(profile: LenderProfile): LenderProfile {
        return lenderProfileJpaRepository.save(profile)
    }

    override fun findByUserId(userId: Long): LenderProfile? {
        return queryFactory
            .selectFrom(lenderProfile)
            .where(lenderProfile.userId.eq(userId))
            .fetchOne()
    }

    override fun existsByUserId(userId: Long): Boolean {
        return queryFactory
            .selectOne()
            .from(lenderProfile)
            .where(lenderProfile.userId.eq(userId))
            .fetchFirst() != null
    }
}
