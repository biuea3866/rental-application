package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.PageQuery

/**
 * 내 대여 목록 조회 조건 — renterId 또는 lenderId 기준 + 상태 필터 + 페이지네이션.
 */
data class RentalQueryCondition(
    val userId: Long,
    val statusFilter: RentalStatus? = null,
    val pageQuery: PageQuery = PageQuery(page = 0, size = 20),
)
