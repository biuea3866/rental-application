package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RejectRentalUseCase(
    private val rentalDomainService: RentalDomainService,
) {

    fun execute(command: RejectRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)
        rentalDomainService.rejectRental(rental, command.userId, command.reason)
    }
}
