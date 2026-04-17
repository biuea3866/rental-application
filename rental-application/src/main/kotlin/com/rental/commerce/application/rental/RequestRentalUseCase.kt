package com.rental.commerce.application.rental

import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.rental.RentalDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * RequestRentalUseCase
 *
 * 대여 신청 UseCase — DomainService만 호출하여 오케스트레이션.
 * 상품 검증은 ProductDomainService + Entity 캡슐화 메서드에 위임.
 * 대여 생성은 RentalDomainService에 위임.
 */
@Service
@Transactional
class RequestRentalUseCase(
    private val rentalDomainService: RentalDomainService,
    private val productDomainService: ProductDomainService,
) {

    fun execute(command: RequestRentalCommand): RequestRentalResult {
        val product = productDomainService.getProductById(command.productId)
        product.validateNotOwnedBy(command.renterId)
        product.validateAvailableForRental()

        val rental = rentalDomainService.createRental(
            renterId = command.renterId,
            lenderId = product.userId,
            productId = command.productId,
            startDate = command.startDate,
            endDate = command.endDate,
            dailyPrice = command.dailyPrice,
            depositAmount = requireNotNull(product.depositAmount) {
                "상품 보증금 정보가 없습니다. productId=${command.productId}"
            },
            deliveryInfo = command.deliveryInfo,
        )
        return RequestRentalResult.from(rental)
    }
}
