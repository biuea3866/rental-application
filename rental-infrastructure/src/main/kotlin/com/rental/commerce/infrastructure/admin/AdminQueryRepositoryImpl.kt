package com.rental.commerce.infrastructure.admin

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.admin.AdminQueryRepository
import com.rental.commerce.domain.admin.AdminRentalFilter
import com.rental.commerce.domain.admin.AdminRentalRow
import com.rental.commerce.domain.admin.DailyRevenueResult
import com.rental.commerce.domain.admin.WeeklyRevenueResult
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.rental.QRental
import com.rental.commerce.domain.rental.QRentalPayment
import com.rental.commerce.domain.rental.RentalStatus
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * AdminQueryRepositoryImpl — QueryDSL 기반 관리자 집계/통계 조회 구현체.
 *
 * @Query 어노테이션 금지 (harness no-jpa-query 룰 준수)
 */
@Component
class AdminQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : AdminQueryRepository {

    private val rental = QRental.rental
    private val rentalPayment = QRentalPayment.rentalPayment

    override fun countByStatus(): Map<RentalStatus, Long> {
        return queryFactory
            .select(rental.status, rental.id.count())
            .from(rental)
            .groupBy(rental.status)
            .fetch()
            .associate { tuple ->
                val status = requireNotNull(tuple.get(rental.status)) { "status must not be null" }
                val count = tuple.get(rental.id.count()) ?: 0L
                status to count
            }
    }

    override fun getDailyRevenue(startDate: ZonedDateTime, endDate: ZonedDateTime): List<DailyRevenueResult> {
        val paidStatuses = listOf(RentalStatus.PAID, RentalStatus.IN_USE, RentalStatus.RETURNED)

        val rows = queryFactory
            .select(rentalPayment.paidAt, rentalPayment.amount)
            .from(rentalPayment)
            .join(rental).on(rentalPayment.rentalId.eq(rental.id))
            .where(
                rental.status.`in`(paidStatuses),
                rentalPayment.paidAt.goe(startDate),
                rentalPayment.paidAt.loe(endDate),
            )
            .fetch()

        return rows
            .groupBy { tuple ->
                val paidAt = requireNotNull(tuple.get(rentalPayment.paidAt))
                paidAt.truncatedTo(ChronoUnit.DAYS)
            }
            .map { (day, tuples) ->
                DailyRevenueResult(
                    date = day,
                    totalRevenue = tuples.sumOf { it.get(rentalPayment.amount) ?: 0L },
                    rentalCount = tuples.size.toLong(),
                )
            }
            .sortedBy { it.date }
    }

    override fun getWeeklyRevenue(startDate: ZonedDateTime, endDate: ZonedDateTime): List<WeeklyRevenueResult> {
        val paidStatuses = listOf(RentalStatus.PAID, RentalStatus.IN_USE, RentalStatus.RETURNED)

        val rows = queryFactory
            .select(rentalPayment.paidAt, rentalPayment.amount)
            .from(rentalPayment)
            .join(rental).on(rentalPayment.rentalId.eq(rental.id))
            .where(
                rental.status.`in`(paidStatuses),
                rentalPayment.paidAt.goe(startDate),
                rentalPayment.paidAt.loe(endDate),
            )
            .fetch()

        return rows
            .groupBy { tuple ->
                val paidAt = requireNotNull(tuple.get(rentalPayment.paidAt))
                val dayOfWeek = paidAt.dayOfWeek.value // 1=MON .. 7=SUN
                paidAt.truncatedTo(ChronoUnit.DAYS).minusDays((dayOfWeek - 1).toLong())
            }
            .map { (weekStart, tuples) ->
                WeeklyRevenueResult(
                    weekStart = weekStart,
                    totalRevenue = tuples.sumOf { it.get(rentalPayment.amount) ?: 0L },
                    rentalCount = tuples.size.toLong(),
                )
            }
            .sortedBy { it.weekStart }
    }

    override fun findAllRentals(filter: AdminRentalFilter): PageResult<AdminRentalRow> {
        val whereClause = buildFilterCondition(filter)

        val totalCount = queryFactory
            .select(rental.id.count())
            .from(rental)
            .where(whereClause)
            .fetchOne() ?: 0L

        if (totalCount == 0L) {
            return PageResult(content = emptyList(), totalElements = 0L, totalPages = 0)
        }

        val content = queryFactory
            .select(
                Projections.constructor(
                    AdminRentalRow::class.java,
                    rental.id,
                    rental.renterId,
                    rental.lenderId,
                    rental.productId,
                    rental.status,
                    rental.totalAmount,
                    rental.requestedAt,
                )
            )
            .from(rental)
            .where(whereClause)
            .orderBy(rental.requestedAt.desc())
            .offset((filter.page * filter.size).toLong())
            .limit(filter.size.toLong())
            .fetch()

        val totalPages = ceil(totalCount.toDouble() / filter.size).toInt()

        return PageResult(
            content = content,
            totalElements = totalCount,
            totalPages = totalPages,
        )
    }

    // ── private helpers ──────────────────────────────────────────

    private fun buildFilterCondition(filter: AdminRentalFilter): BooleanBuilder {
        val builder = BooleanBuilder()
        filter.status?.let { builder.and(rental.status.eq(it)) }
        filter.renterId?.let { builder.and(rental.renterId.eq(it)) }
        filter.lenderId?.let { builder.and(rental.lenderId.eq(it)) }
        return builder
    }
}
