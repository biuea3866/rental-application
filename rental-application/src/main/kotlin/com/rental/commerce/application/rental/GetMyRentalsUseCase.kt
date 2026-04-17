package com.rental.commerce.application.rental

import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalDomainService
import com.rental.commerce.domain.rental.RentalQueryCondition
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GetMyRentalsCommand(
    val userId: Long,
    val statusFilter: RentalStatus? = null,
    val pageQuery: PageQuery = PageQuery(page = 0, size = 20),
)

data class RentalSummaryResult(
    val rentalId: Long,
    val productId: Long,
    val status: RentalStatus,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val totalAmount: Long,
    val depositAmount: Long,
    val requestedAt: ZonedDateTime,
) {
    companion object {
        fun from(rental: Rental): RentalSummaryResult = RentalSummaryResult(
            rentalId = rental.id,
            productId = rental.productId,
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
) {

    fun execute(command: GetMyRentalsCommand): PageResult<RentalSummaryResult> {
        val condition = RentalQueryCondition(
            userId = command.userId,
            statusFilter = command.statusFilter,
            pageQuery = command.pageQuery,
        )
        val page = rentalDomainService.getMyRentals(condition)
        return PageResult(
            content = page.content.map { RentalSummaryResult.from(it) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
