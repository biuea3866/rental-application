package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CancelRentalUseCase
 *
 * REQUESTED, APPROVED, PAID 상태의 대여를 CANCELLED 상태로 전이한다.
 * PAID 상태이면 환불 처리, 상태 전이·save·이벤트 발행은 RentalDomainService.cancelRental()에 위임.
 *
 * @Transactional 은 UseCase 레이어에서만 선언 — harness transaction.default 참고
 */
@Service
@Transactional
class CancelRentalUseCase(
    private val rentalDomainService: RentalDomainService,
) {

    fun execute(command: CancelRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)
        rentalDomainService.cancelRental(rental, command.reason)
    }
}
