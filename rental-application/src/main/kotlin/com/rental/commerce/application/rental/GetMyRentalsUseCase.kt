package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMyRentalsUseCase(
    private val rentalRepository: RentalRepository,
) {

    @Transactional(readOnly = true)
    fun execute(command: GetMyRentalsCommand): List<RentalSummaryResponse> {
        val rentals = when (command.role) {
            RentalRole.RENTER -> rentalRepository.findByRenterIdOrderByRequestedAtDesc(command.userId)
            RentalRole.LENDER -> rentalRepository.findByLenderIdOrderByRequestedAtDesc(command.userId)
        }
        return rentals.map { RentalSummaryResponse.from(it) }
    }
}
