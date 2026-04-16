package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.RentalPaymentRepository
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.rental.RentalStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CancelRentalUseCase
 *
 * REQUESTED, APPROVED, PAID 상태의 대여를 CANCELLED 상태로 전이한다.
 * 권한: 대여자(renter)와 등록자(lender) 양측 모두 취소 가능
 *      — rental.isParticipant() 를 통해 참여자 여부만 확인
 * 상태 전이: rental.cancel() 에서 RentalStatus.canTransitTo() 를 통해 보장
 * 환불: PAID 상태의 경우 PaymentGateway.cancelPayment() → RentalPayment.refund() 호출
 *       IN_USE 이후는 RentalStatus.canTransitTo() 에서 차단됨
 *
 * @Transactional 은 UseCase 레이어에서만 선언 — harness transaction.default 참고
 */
@Service
@Transactional
class CancelRentalUseCase(
    private val rentalDomainService: RentalDomainService,
    private val rentalRepository: RentalRepository,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val rentalEventPublisher: RentalEventPublisher,
) {

    fun execute(command: CancelRentalCommand) {
        val rental = rentalDomainService.getRentalById(command.rentalId)

        // PAID 상태인 경우 결제 취소(환불) 먼저 처리
        if (rental.status == RentalStatus.PAID) {
            val payment = rentalPaymentRepository.findByRentalId(command.rentalId)
            if (payment != null && payment.status == PaymentStatus.COMPLETED) {
                val externalPaymentId = requireNotNull(payment.externalPaymentId) {
                    "결제 외부 키가 없습니다. rentalId=${command.rentalId}"
                }
                paymentGateway.cancelPayment(externalPaymentId, command.reason)
                payment.refund()
                rentalPaymentRepository.save(payment)
            }
        }

        rental.cancel(command.reason)

        rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
    }
}
