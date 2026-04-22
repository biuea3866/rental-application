package com.rental.commerce.domain.dispute

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult

interface DisputeRepository {

    fun save(dispute: Dispute): Dispute

    fun findById(id: Long): Dispute?

    /**
     * 활성(OPEN/UNDER_REVIEW) 분쟁을 조회한다. 동일 rental 에 1건만 존재.
     */
    fun findActiveByRentalId(rentalId: Long): Dispute?

    fun existsActiveByRentalId(rentalId: Long): Boolean

    fun findByOpenerId(openerId: Long, pageQuery: PageQuery): PageResult<Dispute>

    fun findByStatus(status: DisputeStatus, pageQuery: PageQuery): PageResult<Dispute>
}
