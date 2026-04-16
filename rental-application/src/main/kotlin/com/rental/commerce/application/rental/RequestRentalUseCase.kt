package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RequestRentalUseCase(
    private val rentalDomainService: RentalDomainService,
    private val productRepository: ProductRepository,
    private val rentalEventPublisher: RentalEventPublisher,
) {

    fun execute(command: RequestRentalCommand): RequestRentalResult {
        val product = productRepository.findById(command.productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.RESOURCE_NOT_FOUND,
                message = "상품을 찾을 수 없습니다. productId=${command.productId}",
            )

        if (product.isOwnedBy(command.renterId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "자신의 상품은 대여 신청할 수 없습니다.",
            )
        }

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

        rentalEventPublisher.publishAll(rental.pullEvents())

        return RequestRentalResult.from(rental)
    }
}
