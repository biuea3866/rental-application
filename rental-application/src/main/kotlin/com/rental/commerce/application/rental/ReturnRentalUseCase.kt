package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ReturnRentalUseCase
 *
 * IN_USE 상태의 대여를 RETURNED 상태로 전이한다.
 * 권한: 대여자(renter)와 등록자(lender) 양측 모두 반납 처리 가능
 *      — rental.isParticipant() 검증 생략 (양측 모두 허용)
 * 상태 전이: rental.returnRental() 에서 RentalStatus.canTransitTo() 를 통해 보장
 *
 * @Transactional 은 UseCase 레이어에서만 선언 — harness transaction.default 참고
 */
@Service
@Transactional
class ReturnRentalUseCase(
    private val rentalDomainService: RentalDomainService,
    private val rentalRepository: RentalRepository,
    private val rentalEventPublisher: RentalEventPublisher,
) {

    fun execute(command: ReturnRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)

        rental.returnRental()

        rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
    }
}
