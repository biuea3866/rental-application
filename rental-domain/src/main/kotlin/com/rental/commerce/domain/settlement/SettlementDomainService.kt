package com.rental.commerce.domain.settlement

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * SettlementDomainService
 *
 * 정산 생성/조회 비즈니스 로직을 담당한다.
 * - 중복 정산 방지 (findByRentalId 사전 검사)
 * - @Transactional 은 UseCase 레이어에서 선언한다 — harness transaction.default 참고.
 */
@Service
class SettlementDomainService(
    private val settlementRepository: SettlementRepository,
) {

    /**
     * 신규 정산을 생성한다.
     * - 동일 rentalId에 대한 중복 정산 방지
     * - 수수료 계산은 Settlement.create() 내부에서 수행
     */
    fun createSettlement(
        lenderId: Long,
        rentalId: Long,
        amount: BigDecimal,
        commissionRate: BigDecimal,
    ): Settlement {
        settlementRepository.findByRentalId(rentalId)?.let {
            throw SettlementAlreadyExistsException("이미 정산이 생성된 대여입니다. rentalId=$rentalId")
        }
        val settlement = Settlement.create(
            lenderId = lenderId,
            rentalId = rentalId,
            amount = amount,
            commissionRate = commissionRate,
        )
        return settlementRepository.save(settlement)
    }

    /**
     * 특정 등록자의 정산 내역을 조회한다 (페이지네이션 포함).
     */
    fun getMySettlements(lenderId: Long, pageQuery: PageQuery): PageResult<Settlement> {
        return settlementRepository.findByLenderId(lenderId, pageQuery)
    }
}
