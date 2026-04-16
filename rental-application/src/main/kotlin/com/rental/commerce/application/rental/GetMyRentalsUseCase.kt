package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GetMyRentalsCommand(
    val userId: Long,
)

data class RentalSummaryResult(
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
        fun from(rental: Rental): RentalSummaryResult = RentalSummaryResult(
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

@Service
@Transactional(readOnly = true)
class GetMyRentalsUseCase(
    private val rentalRepository: RentalRepository,
) {

    fun execute(command: GetMyRentalsCommand): List<RentalSummaryResult> {
        val rentals = rentalRepository.findAllByRenterIdOrLenderId(
            renterId = command.userId,
            lenderId = command.userId,
        )
        return rentals.map { RentalSummaryResult.from(it) }
    }
}
