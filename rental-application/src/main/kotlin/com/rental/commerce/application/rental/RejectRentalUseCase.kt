package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RejectRentalUseCase(
    private val rentalRepository: RentalRepository,
    private val rentalDomainService: RentalDomainService,
) {

    @Transactional
    fun execute(command: RejectRentalCommand): RentalResponse {
        val rental = rentalDomainService.getOrThrow(command.rentalId)
        rentalDomainService.validateLenderAccess(rental, command.lenderId)
        rental.reject(command.reason)
        val saved = rentalRepository.save(rental)
        return RentalResponse.from(saved)
    }
}
