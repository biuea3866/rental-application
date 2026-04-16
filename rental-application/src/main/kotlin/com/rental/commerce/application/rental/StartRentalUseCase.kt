package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * StartRentalUseCase
 *
 * PAID 상태의 대여를 IN_USE 상태로 전이한다.
 * 권한 검증·상태 전이·save·이벤트 발행은 RentalDomainService.startRental()에 위임.
 *
 * @Transactional 은 UseCase 레이어에서만 선언 — harness transaction.default 참고
 */
@Service
@Transactional
class StartRentalUseCase(
    private val rentalDomainService: RentalDomainService,
) {

    fun execute(command: StartRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)
        rentalDomainService.startRental(rental, command.userId)
    }
}
