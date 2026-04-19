package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubmitProductUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional
    fun execute(command: SubmitProductCommand): ProductSubmitResponse {
        val savedProduct = productDomainService.submit(
            productId = command.productId,
            userId = command.userId,
        )

        return ProductSubmitResponse.from(savedProduct)
    }
}
