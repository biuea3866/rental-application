package com.rental.commerce.domain.admin

import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime

data class DashboardResult(
    val totalRentals: Long,
    val statusCounts: Map<RentalStatus, Long>,
    val revenue: Long,
)

data class AdminRentalResult(
    val rentalId: Long,
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    val status: RentalStatus,
    val totalAmount: Long,
    val requestedAt: ZonedDateTime,
) {
    companion object {
        fun from(row: AdminRentalRow): AdminRentalResult = AdminRentalResult(
            rentalId = row.rentalId,
            renterId = row.renterId,
            lenderId = row.lenderId,
            productId = row.productId,
            status = row.status,
            totalAmount = row.totalAmount,
            requestedAt = row.requestedAt,
        )
    }
}
