package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GetRentalDetailCommand(
    val rentalId: Long,
    val userId: Long,
)

data class RentalDetailResult(
    val rentalId: Long,
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    val status: RentalStatus,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long,
    val deliveryInfo: DeliveryInfo,
    val cancelReason: String?,
    val requestedAt: ZonedDateTime,
    val approvedAt: ZonedDateTime?,
    val paidAt: ZonedDateTime?,
    val startedAt: ZonedDateTime?,
    val returnedAt: ZonedDateTime?,
    val cancelledAt: ZonedDateTime?,
) {
    companion object {
        fun from(rental: Rental): RentalDetailResult = RentalDetailResult(
            rentalId = rental.id,
            renterId = rental.renterId,
            lenderId = rental.lenderId,
            productId = rental.productId,
            status = rental.status,
            startDate = rental.startDate,
            endDate = rental.endDate,
            totalAmount = rental.totalAmount,
            depositAmount = rental.depositAmount,
            deliveryInfo = rental.deliveryInfo,
            cancelReason = rental.cancelReason,
            requestedAt = rental.requestedAt,
            approvedAt = rental.approvedAt,
            paidAt = rental.paidAt,
            startedAt = rental.startedAt,
            returnedAt = rental.returnedAt,
            cancelledAt = rental.cancelledAt,
        )
    }
}

@Service
@Transactional(readOnly = true)
class GetRentalDetailUseCase(
    private val rentalDomainService: RentalDomainService,
) {

    fun execute(command: GetRentalDetailCommand): RentalDetailResult {
        val rental = rentalDomainService.getRentalById(command.rentalId)
        rental.verifyParticipant(command.userId)
        return RentalDetailResult.from(rental)
    }
}
