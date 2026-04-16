package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.RentalPeriodConflictException
import com.rental.commerce.domain.common.ResourceNotFoundException
import com.rental.commerce.domain.common.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service
class RentalDomainService(
    private val rentalRepository: RentalRepository,
) {

    /**
     * 대여 기간 중복 검증.
     * 같은 상품에 CANCELLED를 제외한 활성 대여가 겹치면 RentalPeriodConflictException 발생.
     */
    @Transactional(readOnly = true)
    fun validatePeriodAvailability(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
    ) {
        val hasConflict = rentalRepository.existsOverlappingRental(productId, startDate, endDate)
        if (hasConflict) {
            throw RentalPeriodConflictException(
                "해당 기간에 이미 대여 신청이 존재합니다 (productId=$productId, start=$startDate, end=$endDate)",
            )
        }
    }

    /**
     * 대여 총액 계산: 일 단가 × 일수
     * 일수 = ceil((endDate - startDate) in days)
     */
    fun calculateTotalAmount(
        dailyPrice: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
    ): Long {
        val days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate())
        return dailyPrice * days
    }

    /**
     * 대여 조회 (존재하지 않으면 예외)
     */
    fun getRentalById(rentalId: Long): Rental {
        return rentalRepository.findById(rentalId)
            ?: throw ResourceNotFoundException(
                errorCode = ErrorCode.RENTAL_NOT_FOUND,
                message = "대여 정보를 찾을 수 없습니다 (id=$rentalId)",
            )
    }
}
