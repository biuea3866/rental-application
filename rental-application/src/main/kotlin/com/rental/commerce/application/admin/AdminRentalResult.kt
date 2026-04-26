package com.rental.commerce.application.admin

import com.rental.commerce.domain.admin.AdminRentalResult
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.RentalStatus
import java.time.ZonedDateTime

/**
 * 관리자 대여 목록 응답 — Application 레이어 결과 타입.
 */
data class AdminRentalResponse(
    val rentalId: Long,
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    val status: RentalStatus,
    val totalAmount: Long,
    val requestedAt: ZonedDateTime,
) {
    companion object {
        fun from(domain: AdminRentalResult): AdminRentalResponse = AdminRentalResponse(
            rentalId = domain.rentalId,
            renterId = domain.renterId,
            lenderId = domain.lenderId,
            productId = domain.productId,
            status = domain.status,
            totalAmount = domain.totalAmount,
            requestedAt = domain.requestedAt,
        )
    }
}

data class AdminRentalPageResponse(
    val content: List<AdminRentalResponse>,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(pageResult: PageResult<AdminRentalResult>): AdminRentalPageResponse =
            AdminRentalPageResponse(
                content = pageResult.content.map { AdminRentalResponse.from(it) },
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages,
            )
    }
}
