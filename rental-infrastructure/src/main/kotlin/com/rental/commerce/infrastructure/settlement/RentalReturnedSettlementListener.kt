package com.rental.commerce.infrastructure.settlement

import com.rental.commerce.domain.common.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.domain.settlement.SettlementDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.math.BigDecimal

/**
 * RentalReturnedSettlementListener
 *
 * RETURNED 이벤트 수신 시 정산을 자동 생성한다.
 * Observer 패턴 구현체 — harness design_patterns.observer 규칙 준수.
 *
 * - @TransactionalEventListener(AFTER_COMMIT): 대여 반납 트랜잭션 커밋 완료 후 처리
 * - 중복 정산 방지는 SettlementDomainService.createSettlement() 내부에서 수행
 * - Repository 직접 호출 금지 → SettlementDomainService 경유
 */
@Component
class RentalReturnedSettlementListener(
    private val settlementDomainService: SettlementDomainService,
) {

    private val logger = LoggerFactory.getLogger(RentalReturnedSettlementListener::class.java)

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: RentalStatusChangedEvent) {
        if (event.toStatus != RentalStatus.RETURNED) return
        if (event.rentalAmount <= 0L) {
            logger.warn("[RentalReturnedSettlementListener] rentalAmount=0, 정산 스킵 rentalId={}", event.rentalId)
            return
        }

        logger.info(
            "[RentalReturnedSettlementListener] 정산 생성 시작 rentalId={} lenderId={}",
            event.rentalId,
            event.lenderId,
        )

        runCatching {
            settlementDomainService.createSettlement(
                lenderId = event.lenderId,
                rentalId = event.rentalId,
                amount = BigDecimal.valueOf(event.rentalAmount),
                commissionRate = COMMISSION_RATE,
            )
        }.onSuccess {
            logger.info(
                "[RentalReturnedSettlementListener] 정산 생성 완료 rentalId={} settlementId={}",
                event.rentalId,
                it.id,
            )
        }.onFailure { ex ->
            logger.error(
                "[RentalReturnedSettlementListener] 정산 생성 실패 rentalId={} error={}",
                event.rentalId,
                ex.message,
                ex,
            )
        }
    }

    companion object {
        private val COMMISSION_RATE = BigDecimal("0.10")
    }
}
