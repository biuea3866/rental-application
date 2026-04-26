package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.RentalStatus

/**
 * 내 대여 목록 조회 조건 — renterId 또는 lenderId 기준 + role 필터 + 상태 필터 + 페이지네이션.
 *
 * role = "RENTER" → renterId 기준 조회
 * role = "LENDER" → lenderId 기준 조회
 * role = null → renterId OR lenderId 전체 조회
 */
data class RentalQueryCondition(
    val userId: Long,
    val role: String? = null,
    val statusFilter: RentalStatus? = null,
    val pageQuery: PageQuery = PageQuery(page = 0, size = 20),
)
