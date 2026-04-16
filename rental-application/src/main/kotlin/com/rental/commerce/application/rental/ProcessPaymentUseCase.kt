package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.rental.PaymentApproveRequest
import com.rental.commerce.domain.rental.PaymentGateway
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalPaymentRepository
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.rental.RentalStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProcessPaymentUseCase(
    private val rentalRepository: RentalRepository,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val rentalDomainService: RentalDomainService,
) {

    @Transactional
    fun execute(command: ProcessPaymentCommand): ProcessPaymentResult {
        val rental = rentalDomainService.getOrThrow(command.rentalId)

        // 멱등성: 이미 결제 완료된 경우 기존 결제 정보 반환 (상태 검증 전에 수행)
        val existingPayment = rentalPaymentRepository.findByRentalId(command.rentalId)
        if (existingPayment != null && existingPayment.status == PaymentStatus.COMPLETED) {
            return ProcessPaymentResult.from(rental, existingPayment)
        }

        // APPROVED 상태 검증
        if (rental.status != RentalStatus.APPROVED) {
            throw BusinessException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                message = "APPROVED 상태의 대여만 결제할 수 있습니다 (현재: ${rental.status})",
            )
        }

        // 금액 검증
        if (command.amount != rental.totalAmount) {
            throw BusinessException(
                errorCode = ErrorCode.AMOUNT_MISMATCH,
                message = "결제 금액(${command.amount})이 대여료(${rental.totalAmount})와 일치하지 않습니다",
            )
        }

        val pendingPayment = RentalPayment.pending(
            rentalId = command.rentalId,
            amount = command.amount,
            paymentMethod = command.paymentMethod,
            orderId = command.orderId,
        )

        return try {
            val approveResult = paymentGateway.approve(
                PaymentApproveRequest(
                    paymentKey = command.paymentKey,
                    orderId = command.orderId,
                    amount = command.amount,
                )
            )

            pendingPayment.complete(approveResult.externalPaymentId)
            rental.markPaid()
            rentalRepository.save(rental)
            val savedPayment = rentalPaymentRepository.save(pendingPayment)

            ProcessPaymentResult.from(rental, savedPayment)
        } catch (e: PaymentFailedException) {
            pendingPayment.fail()
            rentalPaymentRepository.save(pendingPayment)
            throw e
        }
    }
}
