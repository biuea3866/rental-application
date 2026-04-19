package com.rental.commerce.application.product

import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.product.ProductImage
import com.rental.commerce.domain.product.ProductPrice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateProductDraftUseCase(
    private val productDomainService: ProductDomainService,
) {

    @Transactional
    fun execute(command: UpdateProductDraftCommand): ProductDraftResponse {
        val newPrices = command.prices?.map { priceCommand ->
            ProductPrice(
                productId = command.productId,
                rentalUnit = priceCommand.rentalUnit,
                priceAmount = priceCommand.priceAmount,
            )
        }

        val newImages = command.images?.map { imageCommand ->
            ProductImage(
                productId = command.productId,
                objectKey = imageCommand.objectKey,
                originalFilename = imageCommand.originalFilename,
                sortOrder = imageCommand.sortOrder.toShort(),
            )
        }

        val savedProduct = productDomainService.updateDraft(
            productId = command.productId,
            userId = command.userId,
            step = command.step,
            name = command.name,
            description = command.description,
            categoryCode = command.categoryCode,
            condition = command.condition,
            depositAmount = command.depositAmount,
            prices = newPrices,
            images = newImages,
        )

        return ProductDraftResponse.from(savedProduct)
    }
}
