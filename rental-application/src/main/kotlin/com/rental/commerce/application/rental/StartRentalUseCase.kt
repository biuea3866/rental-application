package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StartRentalUseCase(
    private val rentalRepository: RentalRepository,
    private val rentalDomainService: RentalDomainService,
) {

    @Transactional
    fun execute(command: StartRentalCommand): RentalResponse {
        val rental = rentalDomainService.getOrThrow(command.rentalId)
        rentalDomainService.validateLenderAccess(rental, command.lenderId)
        rental.start()
        val saved = rentalRepository.save(rental)
        return RentalResponse.from(saved)
    }
}
