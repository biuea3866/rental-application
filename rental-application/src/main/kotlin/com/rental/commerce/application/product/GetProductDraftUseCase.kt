package com.rental.commerce.application.product

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductPriceRepository
import com.rental.commerce.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetProductDraftUseCase(
    private val productRepository: ProductRepository,
    private val productPriceRepository: ProductPriceRepository,
    private val productImageRepository: ProductImageRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, productId: Long): ProductDraftDetailResponse {
        val product = productRepository.findById(productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=$productId)",
            )

        product.validateOwnership(userId)

        val prices = productPriceRepository.findByProductId(productId)
        val images = productImageRepository.findByProductId(productId)

        return ProductDraftDetailResponse.from(
            product = product,
            prices = prices,
            images = images,
        )
    }
}
