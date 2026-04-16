package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ProcessPaymentUseCase
 *
 * 결제 처리 UseCase — RentalDomainService.processPayment()에 위임한다.
 * 멱등성·권한 검증·상태 전이·Gateway 호출·이벤트 발행은 모두 DomainService에 캡슐화.
 *
 * @Transactional 은 UseCase 레이어에서만 선언 — harness transaction.default 참고.
 */
@Service
@Transactional
class ProcessPaymentUseCase(
    private val rentalDomainService: RentalDomainService,
) {

    fun execute(command: ProcessPaymentCommand): ProcessPaymentResult {
        val rental = rentalDomainService.getRentalById(command.rentalId)
        val payment = rentalDomainService.processPayment(
            rental = rental,
            renterId = command.renterId,
            orderId = command.orderId,
            amount = command.amount,
            paymentKey = command.paymentKey,
            paymentMethod = command.paymentMethod,
        )
        return ProcessPaymentResult.of(
            rentalId = rental.id,
            rentalStatus = rental.status,
            payment = payment,
        )
    }
}
