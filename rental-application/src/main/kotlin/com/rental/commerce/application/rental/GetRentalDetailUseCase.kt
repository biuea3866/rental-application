package com.rental.commerce.application.rental

import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.rental.DeliveryInfo
import com.rental.commerce.domain.rental.PaymentMethod
import com.rental.commerce.domain.rental.PaymentStatus
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalPayment
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.rental.RentalWithPayment
import com.rental.commerce.domain.user.UserDomainService
import java.time.ZonedDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GetRentalDetailCommand(
    val rentalId: Long,
    val userId: Long,
)

data class RentalPaymentResult(
    val paymentId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val status: PaymentStatus,
    val externalPaymentId: String?,
    val orderId: String,
    val paidAt: ZonedDateTime?,
    val refundedAt: ZonedDateTime?,
) {
    companion object {
        fun from(payment: RentalPayment): RentalPaymentResult = RentalPaymentResult(
            paymentId = payment.id,
            amount = payment.amount,
            paymentMethod = payment.paymentMethod,
            status = payment.status,
            externalPaymentId = payment.externalPaymentId,
            orderId = payment.orderId,
            paidAt = payment.paidAt,
            refundedAt = payment.refundedAt,
        )
    }
}

data class RentalDetailResult(
    val rentalId: Long,
    // flat 필드 유지 (하위 호환)
    val renterId: Long,
    val lenderId: Long,
    val productId: Long,
    // BUG-S2-004: FE 기대 중첩 객체
    val renter: UserInfo,
    val lender: UserInfo,
    val product: ProductInfo,
    val status: RentalStatus,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long,
    val deliveryInfo: DeliveryInfo,
    val cancelReason: String?,
    val requestedAt: ZonedDateTime,
    val approvedAt: ZonedDateTime?,
    val paidAt: ZonedDateTime?,
    val startedAt: ZonedDateTime?,
    val returnedAt: ZonedDateTime?,
    val cancelledAt: ZonedDateTime?,
    val payment: RentalPaymentResult?,
) {
    data class UserInfo(val userId: Long, val name: String)

    data class ProductInfo(
        val productId: Long,
        val name: String,
        val thumbnailUrl: String?,
    )

    companion object {
        fun from(
            rentalWithPayment: RentalWithPayment,
            renter: UserInfo,
            lender: UserInfo,
            product: ProductInfo,
        ): RentalDetailResult {
            val rental = rentalWithPayment.rental
            return RentalDetailResult(
                rentalId = rental.id,
                renterId = rental.renterId,
                lenderId = rental.lenderId,
                productId = rental.productId,
                renter = renter,
                lender = lender,
                product = product,
                status = rental.status,
                startDate = rental.startDate,
                endDate = rental.endDate,
                totalAmount = rental.totalAmount,
                depositAmount = rental.depositAmount,
                deliveryInfo = rental.deliveryInfo,
                cancelReason = rental.cancelReason,
                requestedAt = rental.requestedAt,
                approvedAt = rental.approvedAt,
                paidAt = rental.paidAt,
                startedAt = rental.startedAt,
                returnedAt = rental.returnedAt,
                cancelledAt = rental.cancelledAt,
                payment = rentalWithPayment.payment?.let { RentalPaymentResult.from(it) },
            )
        }
    }
}

@Service
@Transactional(readOnly = true)
class GetRentalDetailUseCase(
    private val rentalDomainService: RentalDomainService,
    private val userDomainService: UserDomainService,
    private val productDomainService: ProductDomainService,
) {

    fun execute(command: GetRentalDetailCommand): RentalDetailResult {
        val rentalWithPayment = rentalDomainService.getRentalDetail(command.rentalId)
        rentalWithPayment.rental.verifyParticipant(command.userId)
        val renterUser = userDomainService.findById(rentalWithPayment.rental.renterId)
        val lenderUser = userDomainService.findById(rentalWithPayment.rental.lenderId)
        val product = productDomainService.getProductById(rentalWithPayment.rental.productId)
        return RentalDetailResult.from(
            rentalWithPayment = rentalWithPayment,
            renter = RentalDetailResult.UserInfo(
                userId = requireNotNull(renterUser.id) { "renterId가 없습니다" },
                name = renterUser.name,
            ),
            lender = RentalDetailResult.UserInfo(
                userId = requireNotNull(lenderUser.id) { "lenderId가 없습니다" },
                name = lenderUser.name,
            ),
            product = RentalDetailResult.ProductInfo(
                productId = product.productId,
                name = product.name.orEmpty(),
                thumbnailUrl = null,
            ),
        )
    }
}
