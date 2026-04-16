package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RequestRentalUseCase(
    private val rentalDomainService: RentalDomainService,
) {

    fun execute(command: RequestRentalCommand): RequestRentalResult {
        val rental = rentalDomainService.requestRental(
            renterId = command.renterId,
            productId = command.productId,
            startDate = command.startDate,
            endDate = command.endDate,
            dailyPrice = command.dailyPrice,
            deliveryInfo = command.deliveryInfo,
        )
        return RequestRentalResult.from(rental)
    }
}
