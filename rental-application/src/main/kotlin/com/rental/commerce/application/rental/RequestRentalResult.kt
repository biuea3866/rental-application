package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime

data class RequestRentalResult(
    val rentalId: Long,
    val productId: Long,
    val status: RentalStatus,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long,
    val requestedAt: ZonedDateTime,
) {
    companion object {
        fun from(rental: Rental): RequestRentalResult = RequestRentalResult(
            rentalId = rental.id,
            productId = rental.productId,
            status = rental.status,
            startDate = rental.startDate,
            endDate = rental.endDate,
            totalAmount = rental.totalAmount,
            depositAmount = rental.depositAmount,
            requestedAt = rental.requestedAt,
        )
    }
}
