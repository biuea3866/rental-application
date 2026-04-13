package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteProductUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional
    fun execute(userId: Long, productId: Long) {
        val product = productDomainService.getProductById(productId)

        product.validateOwnership(userId)
        product.delete()
    }
}
