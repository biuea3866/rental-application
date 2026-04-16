package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RejectRentalUseCase(
    private val rentalDomainService: RentalDomainService,
    private val rentalRepository: RentalRepository,
    private val rentalEventPublisher: RentalEventPublisher,
) {

    fun execute(command: RejectRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)

        if (!rental.isOwnedByLender(command.userId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "대여 거절 권한이 없습니다. rentalId=${command.rentalId}",
            )
        }

        rental.reject(command.reason)

        rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
    }
}
