package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalPaymentRepository
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CancelRentalUseCase(
    private val rentalRepository: RentalRepository,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val rentalDomainService: RentalDomainService,
) {

    @Transactional
    fun execute(command: CancelRentalCommand): RentalResponse {
        val rental = rentalDomainService.getOrThrow(command.rentalId)
        rentalDomainService.validateParticipantAccess(rental, command.userId)

        // 결제 완료된 경우 환불 처리
        val existingPayment = rentalPaymentRepository.findByRentalId(command.rentalId)
        if (existingPayment != null && existingPayment.status == PaymentStatus.COMPLETED) {
            val externalPaymentId = existingPayment.externalPaymentId
            if (externalPaymentId != null) {
                paymentGateway.cancel(
                    paymentKey = externalPaymentId,
                    cancelAmount = existingPayment.amount,
                    reason = command.reason,
                )
                existingPayment.refund()
                rentalPaymentRepository.save(existingPayment)
            }
        }

        rental.cancel(command.reason)
        val saved = rentalRepository.save(rental)
        return RentalResponse.from(saved)
    }
}
