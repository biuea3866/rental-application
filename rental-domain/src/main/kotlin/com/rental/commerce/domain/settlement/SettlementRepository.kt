package com.rental.commerce.domain.settlement

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult

interface SettlementRepository {

    fun save(settlement: Settlement): Settlement

    fun findByLenderId(lenderId: Long, pageQuery: PageQuery): PageResult<Settlement>

    fun findByRentalId(rentalId: Long): Settlement?
}
