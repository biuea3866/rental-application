package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.ObjectStorageGateway
import com.rental.commerce.domain.common.PresignedUrlResult
import com.rental.commerce.domain.common.ResourceNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductDomainService(
    private val productRepository: ProductRepository,
    private val productPriceRepository: ProductPriceRepository,
    private val productImageRepository: ProductImageRepository,
    private val objectStorageGateway: ObjectStorageGateway,
) {

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
        )
    }

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

    @Transactional
    fun approve(productId: Long) {
        val product = getProductById(productId)
        product.approve()
    }

    @Transactional
    fun reject(productId: Long, reason: String) {
        val product = getProductById(productId)
        product.reject(reason)
    }

    @Transactional
    fun createDraft(userId: Long, name: String?, categoryCode: String?): Product {
        val product = Product(
            userId = userId,
            name = name,
            categoryCode = categoryCode,
            status = ProductStatus.DRAFT,
            currentDraftStep = 1,
        )
        return productRepository.save(product)
    }

    @Transactional
    fun submit(productId: Long, userId: Long): Product {
        val product = getProductById(productId)
        product.validateOwnership(userId)
        product.submit()
        return productRepository.save(product)
    }

    @Transactional(readOnly = true)
    fun getProductWithPricesAndImages(productId: Long, requestUserId: Long?): ProductAggregate {
        val product = getProductById(productId)

        val isOwner = requestUserId != null && product.isOwnedBy(requestUserId)

        if (!product.isPubliclyVisible() && !isOwner) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                message = "해당 상품에 접근할 권한이 없습니다 (productId=$productId)",
            )
        }

        val prices = productPriceRepository.findByProductId(productId)
        val images = productImageRepository.findByProductId(productId)

        return ProductAggregate(
            product = product,
            prices = prices,
            images = images,
        )
    }

    @Transactional(readOnly = true)
    fun getDraftWithPricesAndImages(productId: Long, userId: Long): ProductAggregate {
        val product = getProductById(productId)
        product.validateOwnership(userId)

        val prices = productPriceRepository.findByProductId(productId)
        val images = productImageRepository.findByProductId(productId)

        return ProductAggregate(
            product = product,
            prices = prices,
            images = images,
        )
    }

    @Transactional(readOnly = true)
    fun search(condition: ProductSearchCondition): Page<Product> {
        return productRepository.search(condition)
    }

    @Transactional(readOnly = true)
    fun getImagesByProductIds(productIds: List<Long>): Map<Long, List<ProductImage>> {
        return productImageRepository.findByProductIdIn(productIds)
            .groupBy { it.productId }
    }

    @Transactional
    fun updateDraft(
        productId: Long,
        userId: Long,
        step: Int,
        name: String?,
        description: String?,
        categoryCode: String?,
        condition: ProductCondition?,
        depositAmount: Long?,
        prices: List<ProductPrice>?,
        images: List<ProductImage>?,
    ): Product {
        val product = getProductById(productId)
        product.validateOwnership(userId)
        product.validateDraftStatus()

        product.updateDraft(
            step = step,
            name = name,
            description = description,
            categoryCode = categoryCode,
            condition = condition,
            depositAmount = depositAmount,
        )

        prices?.let { newPrices ->
            productPriceRepository.deleteByProductId(product.productId)
            productPriceRepository.saveAll(newPrices)
        }

        images?.let { newImages ->
            productImageRepository.deleteByProductId(product.productId)
            productImageRepository.saveAll(newImages)
        }

        return productRepository.save(product)
    }

    fun generatePresignedUrl(bucket: String, fileName: String, contentType: String): PresignedUrlResult {
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException(
                errorCode = ErrorCode.INVALID_FILE_TYPE,
                message = "지원하지 않는 파일 형식입니다: $contentType",
            )
        }

        return objectStorageGateway.generatePresignedUrl(
            bucket = bucket,
            fileName = fileName,
            contentType = contentType,
        )
    }
}
