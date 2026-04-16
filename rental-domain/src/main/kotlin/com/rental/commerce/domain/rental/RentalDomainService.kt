package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode
import java.time.ZonedDateTime

/**
 * RentalDomainService — 상태 전이 / 중복 기간 검사 로직 캡슐화.
 * UseCase는 반드시 Repository를 직접 호출하지 않고 이 서비스를 경유합니다.
 */
class RentalDomainService(
    private val rentalRepository: RentalRepository,
) {

    /**
     * 동일 상품에 대해 요청 기간과 겹치는 ACTIVE 대여가 없는지 검증합니다.
     * @param excludeRentalId null이면 모든 대여를 검사, 값이 있으면 해당 대여 제외
     */
    fun validatePeriodAvailability(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        excludeRentalId: Long? = null,
    ) {
        val activeStatuses = listOf(
            RentalStatus.REQUESTED,
            RentalStatus.APPROVED,
            RentalStatus.PAID,
            RentalStatus.IN_USE,
        )
        val overlapping = rentalRepository.findByProductIdAndStatusIn(productId, activeStatuses)
            .filter { it.rentalId != excludeRentalId }
            .any { existing ->
                startDate < existing.endDate && endDate > existing.startDate
            }

        if (overlapping) {
            throw BusinessException(
                errorCode = ErrorCode.RENTAL_PERIOD_CONFLICT,
                message = "해당 기간(${startDate.toLocalDate()} ~ ${endDate.toLocalDate()})에 이미 대여 신청이 존재합니다",
            )
        }
    }

    /**
     * rentalId로 대여를 조회하고, 없으면 예외를 던집니다.
     */
    fun getOrThrow(rentalId: Long): Rental {
        return rentalRepository.findById(rentalId)
            ?: throw BusinessException(
                errorCode = ErrorCode.RENTAL_NOT_FOUND,
                message = "대여를 찾을 수 없습니다 (id=$rentalId)",
            )
    }

    /**
     * 등록자 권한을 검증합니다.
     */
    fun validateLenderAccess(rental: Rental, userId: Long) {
        if (rental.lenderId != userId) {
            throw BusinessException(
                errorCode = ErrorCode.RENTAL_LENDER_ONLY,
                message = "등록자만 수행할 수 있는 작업입니다 (rentalId=${rental.rentalId})",
            )
        }
    }

    /**
     * 대여자 권한을 검증합니다.
     */
    fun validateRenterAccess(rental: Rental, userId: Long) {
        if (rental.renterId != userId) {
            throw BusinessException(
                errorCode = ErrorCode.RENTAL_RENTER_ONLY,
                message = "대여자만 수행할 수 있는 작업입니다 (rentalId=${rental.rentalId})",
            )
        }
    }

    /**
     * 대여자 또는 등록자 권한을 검증합니다.
     */
    fun validateParticipantAccess(rental: Rental, userId: Long) {
        if (rental.renterId != userId && rental.lenderId != userId) {
            throw BusinessException(
                errorCode = ErrorCode.RENTAL_ACCESS_DENIED,
                message = "해당 대여에 접근할 권한이 없습니다 (rentalId=${rental.rentalId})",
            )
        }
    }
}
