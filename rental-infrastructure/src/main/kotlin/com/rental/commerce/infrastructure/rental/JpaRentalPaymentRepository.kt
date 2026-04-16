package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
import org.springframework.stereotype.Repository

@Repository
class JpaRentalPaymentRepository(
    private val rentalPaymentJpaRepository: RentalPaymentJpaRepository,
) : RentalPaymentRepository {

    override fun save(rentalPayment: RentalPayment): RentalPayment {
        return rentalPaymentJpaRepository.save(rentalPayment)
    }

    override fun findByRentalId(rentalId: Long): RentalPayment? {
        return rentalPaymentJpaRepository.findByRentalId(rentalId)
    }

    override fun existsByExternalPaymentId(externalPaymentId: String): Boolean {
        return rentalPaymentJpaRepository.existsByExternalPaymentId(externalPaymentId)
    }
}
