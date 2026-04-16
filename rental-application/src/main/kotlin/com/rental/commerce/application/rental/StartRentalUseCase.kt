package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * StartRentalUseCase
 *
 * PAID 상태의 대여를 IN_USE 상태로 전이한다.
 * 권한: 등록자(lender)만 시작 가능 — rental.isOwnedByLender() 검증
 * 상태 전이: rental.startRental() 에서 RentalStatus.canTransitTo() 를 통해 보장
 *
 * @Transactional 은 UseCase 레이어에서만 선언 — harness transaction.default 참고
 */
@Service
@Transactional
class StartRentalUseCase(
    private val rentalDomainService: RentalDomainService,
    private val rentalRepository: RentalRepository,
    private val rentalEventPublisher: RentalEventPublisher,
) {

    fun execute(command: StartRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)

        if (!rental.isOwnedByLender(command.userId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "대여 시작 권한이 없습니다. rentalId=${command.rentalId}",
            )
        }

        rental.startRental()

        rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
    }
}
