package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalPaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetRentalDetailUseCase(
    private val rentalDomainService: RentalDomainService,
    private val rentalPaymentRepository: RentalPaymentRepository,
) {

    @Transactional(readOnly = true)
    fun execute(rentalId: Long, userId: Long): RentalDetailResponse {
        val rental = rentalDomainService.getOrThrow(rentalId)
        rentalDomainService.validateParticipantAccess(rental, userId)
        val payment = rentalPaymentRepository.findByRentalId(rentalId)
        return RentalDetailResponse.from(rental, payment)
    }
}
