package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.product.ProductDomainService
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalQueryCondition
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GetMyRentalsCommand(
    val userId: Long,
    val role: String? = null,
    val statusFilter: RentalStatus? = null,
    val pageQuery: PageQuery = PageQuery(page = 0, size = 20),
)

data class RentalSummaryResult(
    val rentalId: Long,
    val productId: Long,
    val productName: String,
    val productThumbnailUrl: String?,
    val status: RentalStatus,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long,
    val requestedAt: ZonedDateTime,
) {
    companion object {
        fun from(rental: Rental, productName: String, productThumbnailUrl: String?): RentalSummaryResult =
            RentalSummaryResult(
                rentalId = rental.id,
                productId = rental.productId,
                productName = productName,
                productThumbnailUrl = productThumbnailUrl,
                status = rental.status,
                startDate = rental.startDate,
                endDate = rental.endDate,
                totalAmount = rental.totalAmount,
                depositAmount = rental.depositAmount,
                requestedAt = rental.requestedAt,
            )
    }
}

@Service
@Transactional(readOnly = true)
class GetMyRentalsUseCase(
    private val rentalDomainService: RentalDomainService,
    private val productDomainService: ProductDomainService,
) {

    fun execute(command: GetMyRentalsCommand): PageResult<RentalSummaryResult> {
        val condition = RentalQueryCondition(
            userId = command.userId,
            role = command.role,
            statusFilter = command.statusFilter,
            pageQuery = command.pageQuery,
        )
        val page = rentalDomainService.getMyRentals(condition)
        val summaries = page.content.map { rental ->
            val product = productDomainService.getProductById(rental.productId)
            RentalSummaryResult.from(rental, product.name.orEmpty(), thumbnailUrlOf(product.productId))
        }
        return PageResult(
            content = summaries,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    // Product 도메인에 썸네일 URL이 없을 경우 null 반환 (ProductImage는 별도 조회 필요)
    private fun thumbnailUrlOf(productId: Long): String? = null
}
