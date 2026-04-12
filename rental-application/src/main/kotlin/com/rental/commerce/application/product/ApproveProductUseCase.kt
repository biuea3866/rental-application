package com.rental.commerce.application.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.product.ProductStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApproveProductUseCase(
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun execute(command: ApproveProductCommand) {
        val product = productRepository.findById(command.productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=${command.productId})",
            )

        if (product.status != ProductStatus.UNDER_REVIEW) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_NOT_UNDER_REVIEW,
                message = "검수 중인 상품만 승인할 수 있습니다 (현재 상태: ${product.status})",
            )
        }

        product.approve()
        productRepository.save(product)
    }
}
