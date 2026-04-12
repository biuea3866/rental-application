package com.rental.commerce.application.product

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteProductUseCase(
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun execute(userId: Long, productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=$productId)",
            )

        product.validateOwnership(userId)
        product.delete()

        productRepository.save(product)
    }
}
