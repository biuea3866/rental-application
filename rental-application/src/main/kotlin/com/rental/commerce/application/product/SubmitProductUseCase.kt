package com.rental.commerce.application.product

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubmitProductUseCase(
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun execute(command: SubmitProductCommand): ProductSubmitResponse {
        val product = productRepository.findById(command.productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=${command.productId})",
            )

        product.validateOwnership(command.userId)
        product.submit()

        val savedProduct = productRepository.save(product)

        return ProductSubmitResponse.from(savedProduct)
    }
}
