package com.rental.commerce.application.product

import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductImageRepository
import com.rental.commerce.domain.product.ProductPrice
import com.rental.commerce.domain.product.ProductPriceRepository
import com.rental.commerce.domain.product.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateProductDraftUseCase(
    private val productRepository: ProductRepository,
    private val productPriceRepository: ProductPriceRepository,
    private val productImageRepository: ProductImageRepository,
) {

    @Transactional
    fun execute(command: UpdateProductDraftCommand): ProductDraftResponse {
        val product = productRepository.findById(command.productId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.PRODUCT_NOT_FOUND,
                message = "상품을 찾을 수 없습니다 (id=${command.productId})",
            )

        product.validateOwnership(command.userId)
        product.validateDraftStatus()

        product.updateDraft(
            step = command.step,
            name = command.name,
            description = command.description,
            categoryCode = command.categoryCode,
            condition = command.condition,
            depositAmount = command.depositAmount,
        )

        command.prices?.let { prices ->
            productPriceRepository.deleteByProductId(product.productId)
            val newPrices = prices.map { priceCommand ->
                ProductPrice(
                    productId = product.productId,
                    rentalUnit = priceCommand.rentalUnit,
                    priceAmount = priceCommand.priceAmount,
                )
            }
            productPriceRepository.saveAll(newPrices)
        }

        command.images?.let { images ->
            productImageRepository.deleteByProductId(product.productId)
            val newImages = images.map { imageCommand ->
                ProductImage(
                    productId = product.productId,
                    objectKey = imageCommand.objectKey,
                    originalFilename = imageCommand.originalFilename,
                    sortOrder = imageCommand.sortOrder.toShort(),
                )
            }
            productImageRepository.saveAll(newImages)
        }

        val savedProduct = productRepository.save(product)

        return ProductDraftResponse.from(savedProduct)
    }

}
