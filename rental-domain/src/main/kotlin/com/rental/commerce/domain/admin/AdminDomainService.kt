package com.rental.commerce.domain.admin

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.user.UserRepository
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

/**
 * AdminDomainService — 관리자 전용 도메인 서비스.
 *
 * - getDashboard(): 대여 현황 집계 + 매출 통계 (최근 7일)
 * - getAllRentals(): 전체 대여 목록 (관리자 필터 + 페이지네이션)
 * - suspendUser(): 사용자 정지
 * - activateUser(): 사용자 활성화
 *
 * @Transactional 은 UseCase 레이어에서 선언한다.
 */
@Service
class AdminDomainService(
    private val adminQueryRepository: AdminQueryRepository,
    private val userRepository: UserRepository,
) {

    fun getDashboard(): DashboardResult {
        val statusCounts = adminQueryRepository.countByStatus()
        val totalRentals = statusCounts.values.sum()
        val now = ZonedDateTime.now()
        val weeklyRevenue = adminQueryRepository.getWeeklyRevenue(
            startDate = now.minusDays(27),
            endDate = now,
        )
        val revenue = weeklyRevenue.sumOf { it.totalRevenue }
        return DashboardResult(
            totalRentals = totalRentals,
            statusCounts = statusCounts,
            revenue = revenue,
        )
    }

    fun getAllRentals(filter: AdminRentalFilter): PageResult<AdminRentalResult> {
        val rows = adminQueryRepository.findAllRentals(filter)
        return PageResult(
            content = rows.content.map { AdminRentalResult.from(it) },
            totalElements = rows.totalElements,
            totalPages = rows.totalPages,
        )
    }

    fun suspendUser(userId: Long) {
        val user = userRepository.findById(userId)
            ?: throw BusinessException(
                errorCode = ErrorCode.USER_NOT_FOUND,
                message = "사용자를 찾을 수 없습니다. userId=$userId",
            )
        user.suspend()
        userRepository.save(user)
    }

    fun activateUser(userId: Long) {
        val user = userRepository.findById(userId)
            ?: throw BusinessException(
                errorCode = ErrorCode.USER_NOT_FOUND,
                message = "사용자를 찾을 수 없습니다. userId=$userId",
            )
        user.activate()
        userRepository.save(user)
    }
}
