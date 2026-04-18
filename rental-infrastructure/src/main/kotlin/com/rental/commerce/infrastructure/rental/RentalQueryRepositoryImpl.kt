package com.rental.commerce.infrastructure.rental

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.rental.QRental
import com.rental.commerce.domain.rental.QRentalPayment
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalQueryCondition
import com.rental.commerce.domain.rental.RentalQueryRepository
import com.rental.commerce.domain.rental.RentalWithPayment
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import kotlin.math.ceil

/**
 * RentalQueryRepositoryImpl — QueryDSL 기반 대여 조회 구현체.
 *
 * - findMyRentals: renterId OR lenderId 기준 + 상태 필터 + 페이지네이션
 * - findRentalWithPayment: rentalId 기준 Rental + RentalPayment LEFT JOIN
 *
 * @Query 어노테이션 금지 (harness no-jpa-query 룰 준수)
 */
@Component
class RentalQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : RentalQueryRepository {

    private val rental = QRental.rental
    private val rentalPayment = QRentalPayment.rentalPayment

    override fun findMyRentals(condition: RentalQueryCondition): PageResult<Rental> {
        val pageable = PageRequest.of(condition.pageQuery.page, condition.pageQuery.size)
        val whereClause = buildMyRentalsCondition(condition)

        val totalCount = queryFactory
            .select(rental.countDistinct())
            .from(rental)
            .where(whereClause)
            .fetchOne() ?: 0L

        if (totalCount == 0L) {
            return PageResult(content = emptyList(), totalElements = 0L, totalPages = 0)
        }

        val content = queryFactory
            .selectFrom(rental)
            .where(whereClause)
            .orderBy(rental.requestedAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val totalPages = ceil(totalCount.toDouble() / pageable.pageSize).toInt()

        return PageResult(
            content = content,
            totalElements = totalCount,
            totalPages = totalPages,
        )
    }

    override fun findRentalWithPayment(rentalId: Long): RentalWithPayment? {
        val foundRental: Rental = queryFactory
            .selectFrom(rental)
            .where(rental.id.eq(rentalId))
            .fetchOne() ?: return null

        val foundPayment = queryFactory
            .selectFrom(rentalPayment)
            .where(rentalPayment.rentalId.eq(rentalId))
            .fetchOne()

        return RentalWithPayment(
            rental = foundRental,
            payment = foundPayment,
        )
    }

    // ── private helpers ──────────────────────────────────────────

    private fun buildMyRentalsCondition(condition: RentalQueryCondition): BooleanBuilder {
        val builder = BooleanBuilder()

        when (condition.role?.uppercase()) {
            "RENTER" -> builder.and(rental.renterId.eq(condition.userId))
            "LENDER" -> builder.and(rental.lenderId.eq(condition.userId))
            else -> builder.and(
                rental.renterId.eq(condition.userId)
                    .or(rental.lenderId.eq(condition.userId))
            )
        }

        condition.statusFilter?.let { status ->
            builder.and(rental.status.eq(status))
        }

        return builder
    }
}
