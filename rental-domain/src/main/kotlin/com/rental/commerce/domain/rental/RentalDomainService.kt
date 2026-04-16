package com.rental.commerce.domain.rental

import com.rental.commerce.domain.common.RentalNotFoundException
import com.rental.commerce.domain.common.RentalPeriodConflictException
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * RentalDomainService
 *
 * 여러 Entity/VO를 조합하는 도메인 서비스 — 하네스 architecture_convention.domain.rules 준수
 * - 기간 중복 검증 (validatePeriodAvailability)
 * - 금액 계산 (calculateTotalAmount)
 * - 대여 조회 (getRentalById)
 * - 대여 생성 with 기간 검증 (createRental)
 *
 * @Transactional 은 UseCase 레이어에서 선언한다 — 하네스 transaction.default 참고
 */
@Service
class RentalDomainService(
    private val rentalRepository: RentalRepository,
) {

    /**
     * 대여 기간 중복 여부를 검증한다.
     *
     * @param productId     대상 상품 ID
     * @param startDate     대여 시작일
     * @param endDate       대여 종료일
     * @param excludeRentalId 수정 시 제외할 대여 ID (신규 생성 시 null)
     * @throws RentalPeriodConflictException 기간 중복 대여가 존재하는 경우
     */
    fun validatePeriodAvailability(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        excludeRentalId: Long? = null,
    ) {
        val hasConflict = rentalRepository.existsOverlappingRental(
            productId = productId,
            startDate = startDate,
            endDate = endDate,
            excludeRentalId = excludeRentalId,
        )
        if (hasConflict) {
            throw RentalPeriodConflictException()
        }
    }

    /**
     * 대여 총액을 계산한다.
     *
     * totalAmount = dailyPrice × rentalDays + depositAmount
     *
     * @param dailyPrice    일 단가
     * @param startDate     대여 시작일
     * @param endDate       대여 종료일
     * @param depositAmount 보증금
     * @return 총 결제 금액
     * @throws IllegalArgumentException 대여 일수가 1일 미만인 경우
     */
    fun calculateTotalAmount(
        dailyPrice: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        depositAmount: Long,
    ): Long {
        val rentalDays = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate())
        require(rentalDays > 0) {
            "대여 종료일은 시작일보다 최소 1일 이후여야 합니다. startDate=$startDate, endDate=$endDate"
        }
        return dailyPrice * rentalDays + depositAmount
    }

    /**
     * ID로 대여를 조회한다.
     *
     * @param rentalId 조회할 대여 ID
     * @return 조회된 Rental
     * @throws RentalNotFoundException 해당 ID의 대여가 없는 경우
     */
    fun getRentalById(rentalId: Long): Rental {
        return rentalRepository.findById(rentalId)
            ?: throw RentalNotFoundException("대여를 찾을 수 없습니다. rentalId=$rentalId")
    }

    /**
     * 사용자가 대여자 또는 등록자로 참여한 모든 대여를 조회한다.
     *
     * @param userId 조회할 사용자 ID (대여자 또는 등록자)
     * @return 사용자가 참여한 대여 목록
     */
    fun getRentalsByUserId(userId: Long): List<Rental> {
        return rentalRepository.findAllByRenterIdOrLenderId(
            renterId = userId,
            lenderId = userId,
        )
    }

    /**
     * 신규 대여를 생성한다 — 기간 중복 검증 포함.
     *
     * @throws RentalPeriodConflictException 기간 중복 시
     * @throws IllegalArgumentException      대여 일수 이상 시
     */
    fun createRental(
        renterId: Long,
        lenderId: Long,
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        dailyPrice: Long,
        depositAmount: Long,
        deliveryInfo: DeliveryInfo,
    ): Rental {
        validatePeriodAvailability(productId, startDate, endDate)

        val totalAmount = calculateTotalAmount(
            dailyPrice = dailyPrice,
            startDate = startDate,
            endDate = endDate,
            depositAmount = depositAmount,
        )

        val rental = Rental.create(
            renterId = renterId,
            lenderId = lenderId,
            productId = productId,
            startDate = startDate,
            endDate = endDate,
            totalAmount = totalAmount,
            depositAmount = depositAmount,
            deliveryInfo = deliveryInfo,
        )

        return rentalRepository.save(rental)
    }
}
