package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductDomainService(
    private val productRepository: ProductRepository,
) {

    fun findById(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=$productId)",
            )
    }

    @Transactional
    fun approve(productId: Long) {
        val product = findById(productId)
        product.approve()
    }

    @Transactional
    fun reject(productId: Long, reason: String) {
        val product = findById(productId)
        product.reject(reason)
    }
}
