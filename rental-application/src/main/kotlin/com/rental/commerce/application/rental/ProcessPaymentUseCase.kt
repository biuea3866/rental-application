package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
import com.rental.commerce.domain.rental.RentalStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ProcessPaymentUseCase
 *
 * 결제 처리 + 멱등성 보장 UseCase.
 *
 * - APPROVED 상태 대여에 대해 PaymentGateway 를 통해 결제를 승인한다.
 * - 이미 결제가 완료된 경우(멱등성) 기존 RentalPayment 를 반환한다.
 * - 결제 실패 시 RentalPayment(FAILED) 를 저장하고 PaymentFailedException 을 던진다.
 * - 권한 검증: 대여 신청자(renterId)만 결제 가능 — Entity 메서드 rental.isRequestedByRenter() 사용.
 * - 상태 전이 검증: rental.markPaid() 에서 RentalStatus.canTransitTo() 를 통해 보장.
 *
 * @Transactional 은 UseCase 레이어에서만 선언 — harness transaction.default 참고.
 */
@Service
@Transactional
class ProcessPaymentUseCase(
    private val rentalDomainService: RentalDomainService,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val rentalEventPublisher: RentalEventPublisher,
) {

    fun execute(command: ProcessPaymentCommand): ProcessPaymentResult {
        val rental = rentalDomainService.getRentalById(command.rentalId)

        // 권한 검증 — Entity 메서드 사용 (필드 직접 비교 금지)
        if (!rental.isRequestedByRenter(command.renterId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "결제 권한이 없습니다. rentalId=${command.rentalId}",
            )
        }

        // 멱등성: 이미 결제된 rental 이면 기존 결제 반환
        val existingPayment = rentalPaymentRepository.findByRentalId(command.rentalId)
        if (existingPayment != null) {
            return ProcessPaymentResult.of(
                rentalId = command.rentalId,
                rentalStatus = rental.status,
                payment = existingPayment,
            )
        }

        // 상태 전이 가능 여부를 Gateway 호출 전에 검증 — 불필요한 외부 API 호출 방지
        // RentalStatus.canTransitTo() 를 통해 APPROVED 이외 상태이면 InvalidStateTransitionException 발생
        if (!rental.status.canTransitTo(RentalStatus.PAID)) {
            throw InvalidStateTransitionException(
                "${rental.status.name}에서 PAID(으)로 전이할 수 없습니다"
            )
        }

        val paymentRequest = PaymentApproveRequest(
            orderId = command.orderId,
            amount = command.amount,
            paymentKey = command.paymentKey,
        )

        val paymentResult = paymentGateway.requestPayment(paymentRequest)

        if (!paymentResult.success) {
            val failedPayment = RentalPayment.create(
                rentalId = command.rentalId,
                amount = command.amount,
                paymentMethod = command.paymentMethod,
                orderId = command.orderId,
            )
            failedPayment.fail()
            rentalPaymentRepository.save(failedPayment)
            throw PaymentFailedException(
                message = paymentResult.failureMessage ?: ErrorCode.PAYMENT_FAILED.message
            )
        }

        rental.markPaid()

        val completedPayment = RentalPayment.create(
            rentalId = command.rentalId,
            amount = command.amount,
            paymentMethod = command.paymentMethod,
            orderId = command.orderId,
        )
        completedPayment.complete(paymentResult.paymentKey)

        val savedPayment = rentalPaymentRepository.save(completedPayment)

        rentalEventPublisher.publishAll(rental.pullEvents())

        return ProcessPaymentResult.of(
            rentalId = rental.id,
            rentalStatus = RentalStatus.PAID,
            payment = savedPayment,
        )
    }
}
