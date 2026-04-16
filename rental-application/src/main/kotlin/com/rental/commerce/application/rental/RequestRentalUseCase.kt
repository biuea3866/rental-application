package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RequestRentalUseCase(
    private val rentalRepository: RentalRepository,
    private val rentalDomainService: RentalDomainService,
) {

    @Transactional
    fun execute(command: RequestRentalCommand): RentalResponse {
        rentalDomainService.validatePeriodAvailability(
            productId = command.productId,
            startDate = command.startDate,
            endDate = command.endDate,
        )

        val orderId = "RC-${command.productId}-${System.currentTimeMillis()}"

        val rental = Rental.create(
            renterId = command.renterId,
            lenderId = command.lenderId,
            productId = command.productId,
            startDate = command.startDate,
            endDate = command.endDate,
            totalAmount = command.totalAmount,
            depositAmount = command.depositAmount,
            orderId = orderId,
            deliveryInfo = command.deliveryInfo.toDomain(),
        )

        val saved = rentalRepository.save(rental)

        return RentalResponse.from(saved)
    }
}
