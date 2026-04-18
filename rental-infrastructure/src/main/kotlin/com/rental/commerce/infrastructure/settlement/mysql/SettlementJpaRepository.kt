package com.rental.commerce.infrastructure.settlement.mysql

import com.rental.commerce.domain.settlement.Settlement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementJpaRepository : JpaRepository<Settlement, Long> {

    fun findByRentalId(rentalId: Long): Settlement?

    fun findAllByLenderId(lenderId: Long, pageable: Pageable): Page<Settlement>
}
