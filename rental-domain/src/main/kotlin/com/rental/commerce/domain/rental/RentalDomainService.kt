package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.common.PaymentFailedException
import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.common.RentalPeriodConflictException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductRepository
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * RentalDomainService
 *
 * 여러 Entity/VO를 조합하는 도메인 서비스 — 하네스 architecture_convention.domain.rules 준수
 * - 기간 중복 검증 (validatePeriodAvailability)
 * - 금액 계산 (calculateTotalAmount)
 * - 대여 조회 (getRentalById)
 * - 대여 생성 with 기간 검증 (createRental)
 * - 대여 신청 (requestRental) — 상품 검증 포함
 * - 대여 승인/거절/시작/반납/취소/결제처리
 *   — Repository·Gateway·Publisher 조합이 필요한 오케스트레이션을 이 서비스에 캡슐화
 *
 * @Transactional 은 UseCase 레이어에서 선언한다 — 하네스 transaction.default 참고
 */
@Service
class RentalDomainService(
    private val rentalRepository: RentalRepository,
    private val rentalPaymentRepository: RentalPaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val rentalEventPublisher: RentalEventPublisher,
    private val productRepository: ProductRepository,
) {

    /**
     * 대여 기간 중복 여부를 검증한다.
     *
     * @param productId     대상 상품 ID
     * @param startDate     대여 시작일
     * @param endDate       대여 종료일
     * @param excludeRentalId 수정 시 제외할 대여 ID (신규 생성 시 null)
     * @throws RentalPeriodConflictException 기간 중복 대여가 존재하는 경우
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
     *
     * totalAmount = dailyPrice × rentalDays + depositAmount
     *
     * @param dailyPrice    일 단가
     * @param startDate     대여 시작일
     * @param endDate       대여 종료일
     * @param depositAmount 보증금
     * @return 총 결제 금액
     * @throws IllegalArgumentException 대여 일수가 1일 미만인 경우
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
     *
     * @param rentalId 조회할 대여 ID
     * @return 조회된 Rental
     * @throws RentalNotFoundException 해당 ID의 대여가 없는 경우
     */
    fun getRentalById(rentalId: Long): Rental {
        return rentalRepository.findById(rentalId)
            ?: throw RentalNotFoundException("대여를 찾을 수 없습니다. rentalId=$rentalId")
    }

    /**
     * 사용자가 대여자 또는 등록자로 참여한 모든 대여를 조회한다.
     *
     * @param userId 조회할 사용자 ID (대여자 또는 등록자)
     * @return 사용자가 참여한 대여 목록
     */
    fun getRentalsByUserId(userId: Long): List<Rental> {
        return rentalRepository.findAllByRenterIdOrLenderId(
            renterId = userId,
            lenderId = userId,
        )
    }

    /**
     * 신규 대여를 생성한다 — 기간 중복 검증 포함.
     *
     * @throws RentalPeriodConflictException 기간 중복 시
     * @throws IllegalArgumentException      대여 일수 이상 시
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
     * 대여 신청을 처리한다 — 상품 조회·검증 + createRental() 위임.
     *
     * - 상품이 없으면 ResourceNotFoundException
     * - 자신의 상품에 대여 신청 시 FORBIDDEN BusinessException
     * - 상품이 대여 불가 상태이면 validateAvailableForRental()에서 예외
     *
     * @throws ResourceNotFoundException 상품이 없는 경우
     * @throws BusinessException         자신의 상품에 대여 신청 시
     */
    fun requestRental(
        renterId: Long,
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        dailyPrice: Long,
        deliveryInfo: DeliveryInfo,
    ): Rental {
        val product = productRepository.findById(productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.RESOURCE_NOT_FOUND,
                message = "상품을 찾을 수 없습니다. productId=$productId",
            )

        if (product.isOwnedBy(renterId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "자신의 상품은 대여 신청할 수 없습니다.",
            )
        }

        product.validateAvailableForRental()

        return createRental(
            renterId = renterId,
            lenderId = product.userId,
            productId = productId,
            startDate = startDate,
            endDate = endDate,
            dailyPrice = dailyPrice,
            depositAmount = requireNotNull(product.depositAmount) {
                "상품 보증금 정보가 없습니다. productId=$productId"
            },
            deliveryInfo = deliveryInfo,
        )
    }

    /**
     * 대여를 승인한다 — 권한 검증 + approve() + save + 이벤트 발행.
     *
     * @throws BusinessException FORBIDDEN — 등록자가 아닌 경우
     */
    fun approveRental(rental: Rental, userId: Long): Rental {
        if (!rental.isOwnedByLender(userId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "대여 승인 권한이 없습니다. rentalId=${rental.id}",
            )
        }
        rental.approve()
        val saved = rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
        return saved
    }

    /**
     * 대여를 거절한다 — 권한 검증 + reject() + save + 이벤트 발행.
     *
     * @throws BusinessException FORBIDDEN — 등록자가 아닌 경우
     */
    fun rejectRental(rental: Rental, userId: Long, reason: String): Rental {
        if (!rental.isOwnedByLender(userId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "대여 거절 권한이 없습니다. rentalId=${rental.id}",
            )
        }
        rental.reject(reason)
        val saved = rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
        return saved
    }

    /**
     * 결제를 처리한다 — 멱등성 보장 + PaymentGateway 호출 + RentalPayment save + 이벤트 발행.
     *
     * - 이미 결제된 경우 기존 RentalPayment 반환 (멱등성)
     * - 상태 검증: APPROVED → PAID 전이 가능 여부는 rental.markPaid() 내부에서도 보장되나,
     *   불필요한 외부 API 호출을 막기 위해 사전 검증 수행
     * - 결제 실패 시 RentalPayment(FAILED) 저장 후 PaymentFailedException
     *
     * @throws BusinessException              FORBIDDEN — 대여 신청자가 아닌 경우
     * @throws InvalidStateTransitionException APPROVED 이외 상태에서 결제 시도
     * @throws PaymentFailedException          PG 결제 실패 시
     */
    fun processPayment(
        rental: Rental,
        renterId: Long,
        orderId: String,
        amount: Long,
        paymentKey: String,
        paymentMethod: PaymentMethod,
    ): RentalPayment {
        if (!rental.isRequestedByRenter(renterId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "결제 권한이 없습니다. rentalId=${rental.id}",
            )
        }

        val existingPayment = rentalPaymentRepository.findByRentalId(rental.id)
        if (existingPayment != null) {
            return existingPayment
        }

        if (!rental.status.canTransitTo(RentalStatus.PAID)) {
            throw InvalidStateTransitionException(
                "${rental.status.name}에서 PAID(으)로 전이할 수 없습니다"
            )
        }

        val paymentRequest = PaymentApproveRequest(
            orderId = orderId,
            amount = amount,
            paymentKey = paymentKey,
        )
        val paymentResult = paymentGateway.requestPayment(paymentRequest)

        if (!paymentResult.success) {
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

    /**
     * 대여를 시작한다 — 권한 검증 + startRental() + save + 이벤트 발행.
     *
     * @throws BusinessException FORBIDDEN — 등록자가 아닌 경우
     */
    fun startRental(rental: Rental, userId: Long): Rental {
        if (!rental.isOwnedByLender(userId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "대여 시작 권한이 없습니다. rentalId=${rental.id}",
            )
        }
        rental.startRental()
        val saved = rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
        return saved
    }

    /**
     * 대여를 반납 처리한다 — returnRental() + save + 이벤트 발행.
     */
    fun returnRental(rental: Rental): Rental {
        rental.returnRental()
        val saved = rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
        return saved
    }

    /**
     * 대여를 취소한다 — PAID 상태이면 결제 취소(환불) 후 cancel() + save + 이벤트 발행.
     *
     * - PAID 상태: PaymentGateway.cancelPayment() → RentalPayment.refund() → save
     * - 그 외: 직접 cancel()
     */
    fun cancelRental(rental: Rental, reason: String): Rental {
        if (rental.status == RentalStatus.PAID) {
            val payment = rentalPaymentRepository.findByRentalId(rental.id)
            if (payment != null && payment.status == PaymentStatus.COMPLETED) {
                val externalPaymentId = requireNotNull(payment.externalPaymentId) {
                    "결제 외부 키가 없습니다. rentalId=${rental.id}"
                }
                paymentGateway.cancelPayment(externalPaymentId, reason)
                payment.refund()
                rentalPaymentRepository.save(payment)
            }
        }
        rental.cancel(reason)
        val saved = rentalRepository.save(rental)
        rentalEventPublisher.publishAll(rental.pullEvents())
        return saved
    }
}
