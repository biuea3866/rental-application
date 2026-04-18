package com.rental.commerce.infrastructure.settlement.mysql

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.settlement.Settlement
import com.rental.commerce.domain.settlement.SettlementRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import kotlin.math.ceil

/**
 * SettlementRepositoryImpl — JPA 기반 정산 Repository 구현체.
 *
 * - @Query 어노테이션 사용 금지 (harness no-jpa-query 룰 준수)
 * - findAllByLenderId: Spring Data JPA 쿼리 메서드 + Pageable 사용
 */
@Repository
class SettlementRepositoryImpl(
    private val settlementJpaRepository: SettlementJpaRepository,
) : SettlementRepository {

    override fun save(settlement: Settlement): Settlement {
        return settlementJpaRepository.save(settlement)
    }

    override fun findByLenderId(lenderId: Long, pageQuery: PageQuery): PageResult<Settlement> {
        val pageable = PageRequest.of(
            pageQuery.page,
            pageQuery.size,
            Sort.by(Sort.Direction.DESC, "createdAt"),
        )
        val page = settlementJpaRepository.findAllByLenderId(lenderId, pageable)
        val totalPages = if (page.totalElements == 0L) 0
        else ceil(page.totalElements.toDouble() / pageQuery.size).toInt()

        return PageResult(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = totalPages,
        )
    }

    override fun findByRentalId(rentalId: Long): Settlement? {
        return settlementJpaRepository.findByRentalId(rentalId)
    }
}
