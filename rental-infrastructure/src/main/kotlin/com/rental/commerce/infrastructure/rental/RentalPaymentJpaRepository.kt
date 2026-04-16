package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.RentalPayment
import org.springframework.data.jpa.repository.JpaRepository

interface RentalPaymentJpaRepository : JpaRepository<RentalPayment, Long> {

    fun findByRentalId(rentalId: Long): RentalPayment?

    fun existsByExternalPaymentId(externalPaymentId: String): Boolean
}
