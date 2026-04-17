package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.common.RentalPeriodConflictException
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * RentalDomainService
 *
 * Entity + Port(Repository/Gateway/Publisher) 조합을 수행하는 도메인 서비스.
 * 권한 검증은 Entity 캡슐화 메서드(verifyLenderAuthority 등)에 위임한다.
 * 상품 검증은 도메인 패키지 격리 원칙에 따라 UseCase에서 수행한다.
 *
 * @Transactional 은 UseCase 레이어에서 선언한다 — harness transaction.default 참고
 */
@Service
class RentalDomainService(
    private val rentalRepository: RentalRepository,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val rentalEventPublisher: RentalEventPublisher,
    private val rentalQueryRepository: RentalQueryRepository,
) {

    /**
     * 대여 기간 중복 여부를 검증한다.
     */
    fun validatePeriodAvailability(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        excludeRentalId: Long? = null,
    ) {
        val hasConflict = rentalRepository.existsOverlappingRental(
            productId = productId,
            startDate = startDate,
            endDate = endDate,
            excludeRentalId = excludeRentalId,
        )
        if (hasConflict) {
            throw RentalPeriodConflictException()
        }
    }

    /**
     * 대여 총액을 계산한다.
     * totalAmount = dailyPrice x rentalDays + depositAmount
     */
    fun calculateTotalAmount(
        dailyPrice: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        depositAmount: Long,
    ): Long {
        val rentalDays = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate())
        require(rentalDays > 0) {
            "대여 종료일은 시작일보다 최소 1일 이후여야 합니다. startDate=$startDate, endDate=$endDate"
        }
        return dailyPrice * rentalDays + depositAmount
    }

    /**
     * ID로 대여를 조회한다.
     */
    fun getRentalById(rentalId: Long): Rental {
        return rentalRepository.findById(rentalId)
            ?: throw RentalNotFoundException("대여를 찾을 수 없습니다. rentalId=$rentalId")
    }

    /**
     * 사용자가 대여자 또는 등록자로 참여한 모든 대여를 조회한다.
     */
    fun getRentalsByUserId(userId: Long): List<Rental> {
        return rentalRepository.findAllByRenterIdOrLenderId(
            renterId = userId,
            lenderId = userId,
        )
    }

    /**
     * 내 대여 목록 조회 — 상태 필터 + 페이지네이션 지원 (QueryDSL).
     */
    fun getMyRentals(condition: RentalQueryCondition): PageResult<Rental> {
        return rentalQueryRepository.findMyRentals(condition)
    }

    /**
     * 대여 상세 조회 — Rental + RentalPayment JOIN (QueryDSL).
     */
    fun getRentalDetail(rentalId: Long): RentalWithPayment {
        return rentalQueryRepository.findRentalWithPayment(rentalId)
            ?: throw RentalNotFoundException("대여를 찾을 수 없습니다. rentalId=$rentalId")
    }

    /**
     * 신규 대여를 생성한다 — 기간 중복 검증 포함.
     */
    fun createRental(
        renterId: Long,
        lenderId: Long,
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        dailyPrice: Long,
        depositAmount: Long,
        deliveryInfo: DeliveryInfo,
    ): Rental {
        validatePeriodAvailability(productId, startDate, endDate)

        val totalAmount = calculateTotalAmount(
            dailyPrice = dailyPrice,
            startDate = startDate,
            endDate = endDate,
            depositAmount = depositAmount,
        )

        val rental = Rental.create(
            renterId = renterId,
            lenderId = lenderId,
            productId = productId,
            startDate = startDate,
            endDate = endDate,
            totalAmount = totalAmount,
            depositAmount = depositAmount,
            deliveryInfo = deliveryInfo,
        )

        return rentalRepository.save(rental)
    }

    /**
     * 대여를 승인한다 — Entity 권한 검증 + approve() + save + 이벤트 발행.
     */
    fun approveRental(rental: Rental, userId: Long): Rental {
        rental.verifyLenderAuthority(userId)
        rental.approve()
        return saveAndPublishEvents(rental)
    }

    /**
     * 대여를 거절한다 — Entity 권한 검증 + reject() + save + 이벤트 발행.
     */
    fun rejectRental(rental: Rental, userId: Long, reason: String): Rental {
        rental.verifyLenderAuthority(userId)
        rental.reject(reason)
        return saveAndPublishEvents(rental)
    }

    /**
     * 결제를 처리한다 — 멱등성 보장 + PaymentGateway 호출 + RentalPayment save + 이벤트 발행.
     */
    fun processPayment(
        rental: Rental,
        renterId: Long,
        orderId: String,
        amount: Long,
        paymentKey: String,
        paymentMethod: PaymentMethod,
    ): RentalPayment {
        rental.verifyRenterAuthority(renterId)

        rentalPaymentRepository.findByRentalId(rental.id)?.let { return it }

        validatePaymentTransition(rental)

        val paymentResult = requestExternalPayment(orderId, amount, paymentKey)

        if (!paymentResult.success) {
            return handlePaymentFailure(rental, amount, paymentMethod, orderId, paymentResult)
        }

        return completePayment(rental, amount, paymentMethod, orderId, paymentResult)
    }

    /**
     * 대여를 시작한다 — Entity 권한 검증 + startRental() + save + 이벤트 발행.
     */
    fun startRental(rental: Rental, userId: Long): Rental {
        rental.verifyLenderAuthority(userId)
        rental.startRental()
        return saveAndPublishEvents(rental)
    }

    /**
     * 대여를 반납 처리한다 — returnRental() + save + 이벤트 발행.
     */
    fun returnRental(rental: Rental): Rental {
        rental.returnRental()
        return saveAndPublishEvents(rental)
    }

    /**
     * 대여를 취소한다 — PAID 상태이면 환불 후 cancel() + save + 이벤트 발행.
     */
    fun cancelRental(rental: Rental, reason: String): Rental {
        if (rental.isPaid()) {
            refundIfCompleted(rental, reason)
        }
        rental.cancel(reason)
        return saveAndPublishEvents(rental)
    }

    // ── private helpers ──────────────────────────────────────────

    private fun saveAndPublishEvents(rental: Rental): Rental {
        val saved = rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
        return saved
    }

    private fun validatePaymentTransition(rental: Rental) {
        if (!rental.status.canTransitTo(RentalStatus.PAID)) {
            throw InvalidStateTransitionException(
                "${rental.status.name}에서 PAID(으)로 전이할 수 없습니다"
            )
        }
    }

    private fun requestExternalPayment(
        orderId: String,
        amount: Long,
        paymentKey: String,
    ): PaymentResult {
        return paymentGateway.requestPayment(
            PaymentApproveRequest(
                orderId = orderId,
                amount = amount,
                paymentKey = paymentKey,
            )
        )
    }

    private fun handlePaymentFailure(
        rental: Rental,
        amount: Long,
        paymentMethod: PaymentMethod,
        orderId: String,
        paymentResult: PaymentResult,
    ): Nothing {
        val failedPayment = RentalPayment.create(
            rentalId = rental.id,
            amount = amount,
            paymentMethod = paymentMethod,
            orderId = orderId,
        )
        failedPayment.fail()
        rentalPaymentRepository.save(failedPayment)
        throw PaymentFailedException(
            message = paymentResult.failureMessage ?: ErrorCode.PAYMENT_FAILED.message
        )
    }

    private fun completePayment(
        rental: Rental,
        amount: Long,
        paymentMethod: PaymentMethod,
        orderId: String,
        paymentResult: PaymentResult,
    ): RentalPayment {
        rental.markPaid()

        val completedPayment = RentalPayment.create(
            rentalId = rental.id,
            amount = amount,
            paymentMethod = paymentMethod,
            orderId = orderId,
        )
        completedPayment.complete(paymentResult.paymentKey)

        val savedPayment = rentalPaymentRepository.save(completedPayment)
        rentalEventPublisher.publishAll(rental.pullEvents())
        return savedPayment
    }

    private fun refundIfCompleted(rental: Rental, reason: String) {
        val payment = rentalPaymentRepository.findByRentalId(rental.id) ?: return
        if (payment.status != PaymentStatus.COMPLETED) return

        val externalPaymentId = requireNotNull(payment.externalPaymentId) {
            "결제 외부 키가 없습니다. rentalId=${rental.id}"
        }
        paymentGateway.cancelPayment(externalPaymentId, reason)
        payment.refund()
        rentalPaymentRepository.save(payment)
    }
}
