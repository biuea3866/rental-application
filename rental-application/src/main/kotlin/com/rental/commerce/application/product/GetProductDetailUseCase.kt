package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductPriceRepository
import com.rental.commerce.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetProductDetailUseCase(
    private val productRepository: ProductRepository,
    private val productPriceRepository: ProductPriceRepository,
    private val productImageRepository: ProductImageRepository,
) {

    @Transactional(readOnly = true)
    fun execute(productId: Long, requestUserId: Long?): ProductDetailResponse {
        val product = productRepository.findById(productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=$productId)",
            )

        val isOwner = requestUserId != null && product.userId == requestUserId

        if (!product.isPubliclyVisible() && !isOwner) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                message = "해당 상품에 접근할 권한이 없습니다 (productId=$productId)",
            )
        }

        val prices = productPriceRepository.findByProductId(productId)
        val images = productImageRepository.findByProductId(productId)

        return ProductDetailResponse.from(
            product = product,
            prices = prices,
            images = images,
        )
    }
}
