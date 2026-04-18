package com.rental.commerce.application.settlement

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.settlement.Settlement
import com.rental.commerce.domain.settlement.SettlementDomainService
import com.rental.commerce.domain.settlement.SettlementStatus
import java.math.BigDecimal
import java.time.ZonedDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GetMySettlementsCommand(
    val lenderId: Long,
    val pageQuery: PageQuery = PageQuery(page = 0, size = 20),
)

data class SettlementResult(
    val settlementId: Long,
    val lenderId: Long,
    val rentalId: Long,
    val amount: BigDecimal,
    val commission: BigDecimal,
    val netAmount: BigDecimal,
    val status: SettlementStatus,
    val settledAt: ZonedDateTime?,
) {
    companion object {
        fun from(settlement: Settlement): SettlementResult =
            SettlementResult(
                settlementId = settlement.id,
                lenderId = settlement.lenderId,
                rentalId = settlement.rentalId,
                amount = settlement.amount,
                commission = settlement.commission,
                netAmount = settlement.netAmount,
                status = settlement.status,
                settledAt = settlement.settledAt,
            )
    }
}

@Service
@Transactional(readOnly = true)
class GetMySettlementsUseCase(
    private val settlementDomainService: SettlementDomainService,
) {

    fun execute(command: GetMySettlementsCommand): PageResult<SettlementResult> {
        val page = settlementDomainService.getMySettlements(command.lenderId, command.pageQuery)
        return PageResult(
            content = page.content.map { SettlementResult.from(it) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
