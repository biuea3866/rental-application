package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetProductDetailUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(productId: Long, requestUserId: Long?): ProductDetailResponse {
        val aggregate = productDomainService.getProductWithPricesAndImages(
            productId = productId,
            requestUserId = requestUserId,
        )

        return ProductDetailResponse.from(
            product = aggregate.product,
            prices = aggregate.prices,
            images = aggregate.images,
        )
    }
}
