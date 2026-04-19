package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminRentalFilter
import com.rental.commerce.domain.rental.RentalStatus

/**
 * 관리자 전용 Command 모음.
 *
 * - GetAdminRentalsCommand : 전체 대여 목록 조회 (필터 + 페이지네이션)
 * - SuspendUserCommand     : 사용자 정지
 * - ActivateUserCommand    : 사용자 활성화
 */

data class GetAdminRentalsCommand(
    val status: RentalStatus? = null,
    val renterId: Long? = null,
    val lenderId: Long? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    fun toFilter(): AdminRentalFilter = AdminRentalFilter(
        status = status,
        renterId = renterId,
        lenderId = lenderId,
        page = page,
        size = size,
    )
}

data class SuspendUserCommand(
    val userId: Long,
)

data class ActivateUserCommand(
    val userId: Long,
)
