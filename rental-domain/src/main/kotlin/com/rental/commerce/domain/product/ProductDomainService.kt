package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ProductDomainService(
    private val productRepository: ProductRepository,
) {

    fun getProductById(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=$productId)",
            )
    }

    fun getMyProducts(
        userId: Long,
        pageable: Pageable,
    ): Page<Product> {
        return productRepository.findByUserIdAndStatusNot(
            userId = userId,
            excludeStatus = ProductStatus.DELETED,
            pageable = pageable,
        )
    }
}
