package com.rental.commerce.domain.admin

import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime

/**
 * AdminQueryRepository — 관리자 전용 집계/통계 조회 Port.
 *
 * @Query 금지 — 구현체는 QueryDSL JPAQueryFactory 사용
 */
interface AdminQueryRepository {

    /**
     * 대여 상태별 count 집계.
     * key: RentalStatus, value: 해당 상태의 대여 건수
     */
    fun countByStatus(): Map<RentalStatus, Long>

    /**
     * 일별 매출 통계 (startDate ~ endDate 범위).
     * 결제 완료(PAID/IN_USE/RETURNED) 상태 기준.
     */
    fun getDailyRevenue(startDate: ZonedDateTime, endDate: ZonedDateTime): List<DailyRevenueResult>

    /**
     * 주별 매출 통계 (startDate ~ endDate 범위).
     */
    fun getWeeklyRevenue(startDate: ZonedDateTime, endDate: ZonedDateTime): List<WeeklyRevenueResult>

    /**
     * 전체 대여 목록 — 관리자 필터 + 페이지네이션.
     */
    fun findAllRentals(filter: AdminRentalFilter): PageResult<AdminRentalRow>
}

data class DailyRevenueResult(
    val date: ZonedDateTime,
    val totalRevenue: Long,
    val rentalCount: Long,
)

data class WeeklyRevenueResult(
    val weekStart: ZonedDateTime,
    val totalRevenue: Long,
    val rentalCount: Long,
)

data class AdminRentalRow(
    val rentalId: Long,
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    val status: RentalStatus,
    val totalAmount: Long,
    val requestedAt: ZonedDateTime,
)

data class AdminRentalFilter(
    val status: RentalStatus? = null,
    val renterId: Long? = null,
    val lenderId: Long? = null,
    val page: Int = 0,
    val size: Int = 20,
)
