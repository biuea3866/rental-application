package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface SpringDataRentalPaymentRepository : JpaRepository<RentalPayment, Long> {
    fun findByRentalId(rentalId: Long): RentalPayment?
    fun existsByExternalPaymentId(externalPaymentId: String): Boolean
}

@Repository
class JpaRentalPaymentRepository(
    private val springDataRepo: SpringDataRentalPaymentRepository,
) : RentalPaymentRepository {

    override fun save(rentalPayment: RentalPayment): RentalPayment = springDataRepo.save(rentalPayment)

    override fun findByRentalId(rentalId: Long): RentalPayment? = springDataRepo.findByRentalId(rentalId)

    override fun existsByExternalPaymentId(externalPaymentId: String): Boolean =
        springDataRepo.existsByExternalPaymentId(externalPaymentId)
}
