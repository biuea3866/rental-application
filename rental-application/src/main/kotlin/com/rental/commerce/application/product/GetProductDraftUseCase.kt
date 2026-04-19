package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetProductDraftUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, productId: Long): ProductDraftDetailResponse {
        val aggregate = productDomainService.getDraftWithPricesAndImages(
            productId = productId,
            userId = userId,
        )

        return ProductDraftDetailResponse.from(
            product = aggregate.product,
            prices = aggregate.prices,
            images = aggregate.images,
        )
    }
}
