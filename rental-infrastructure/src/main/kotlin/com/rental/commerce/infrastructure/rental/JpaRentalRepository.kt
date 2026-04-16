package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.rental.RentalStatus
import java.time.ZonedDateTime
import org.springframework.stereotype.Repository

@Repository
class JpaRentalRepository(
    private val rentalJpaRepository: RentalJpaRepository,
) : RentalRepository {

    private val activeStatuses = listOf(
        RentalStatus.REQUESTED,
        RentalStatus.APPROVED,
        RentalStatus.PAID,
        RentalStatus.IN_USE,
    )

    override fun save(rental: Rental): Rental {
        return rentalJpaRepository.save(rental)
    }

    override fun findById(rentalId: Long): Rental? {
        return rentalJpaRepository.findById(rentalId).orElse(null)
    }

    override fun existsOverlappingRental(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
        excludeRentalId: Long?,
    ): Boolean {
        return if (excludeRentalId != null) {
            rentalJpaRepository
                .existsByProductIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                    productId = productId,
                    statuses = activeStatuses,
                    endDate = endDate,
                    startDate = startDate,
                    excludeId = excludeRentalId,
                )
        } else {
            rentalJpaRepository
                .existsByProductIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    productId = productId,
                    statuses = activeStatuses,
                    endDate = endDate,
                    startDate = startDate,
                )
        }
    }

    override fun findAllByRenterIdOrLenderId(renterId: Long, lenderId: Long): List<Rental> {
        return rentalJpaRepository.findAllByRenterIdOrLenderId(
            renterId = renterId,
            lenderId = lenderId,
        )
    }
}
