package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ApproveRentalUseCase(
    private val rentalDomainService: RentalDomainService,
) {

    fun execute(command: ApproveRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)
        rentalDomainService.approveRental(rental, command.userId)
    }
}
