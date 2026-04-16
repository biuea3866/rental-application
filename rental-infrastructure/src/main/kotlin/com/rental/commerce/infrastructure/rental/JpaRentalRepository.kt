package com.rental.commerce.infrastructure.rental

import com.rental.commerce.domain.rental.Rental
import com.rental.commerce.domain.rental.RentalRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

interface SpringDataRentalRepository : JpaRepository<Rental, Long> {

    @org.springframework.data.jpa.repository.Query(
        "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Rental r " +
            "WHERE r.productId = :productId " +
            "AND r.startDate < :endDate " +
            "AND r.endDate > :startDate " +
            "AND r.status <> com.rental.commerce.domain.rental.RentalStatus.CANCELLED"
    )
    fun existsOverlappingRental(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
    ): Boolean

    @org.springframework.data.jpa.repository.Query(
        "SELECT r FROM Rental r WHERE r.renterId = :userId OR r.lenderId = :userId ORDER BY r.createdAt DESC"
    )
    fun findAllByRenterIdOrLenderId(userId: Long, pageable: Pageable): Page<Rental>
}

@Repository
class JpaRentalRepository(
    private val springDataRepo: SpringDataRentalRepository,
) : RentalRepository {

    override fun save(rental: Rental): Rental = springDataRepo.save(rental)

    override fun findById(rentalId: Long): Rental? = springDataRepo.findById(rentalId).orElse(null)

    override fun existsOverlappingRental(
        productId: Long,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime,
    ): Boolean = springDataRepo.existsOverlappingRental(productId, startDate, endDate)

    override fun findAllByRenterIdOrLenderId(
        userId: Long,
        pageable: Pageable,
    ): Page<Rental> = springDataRepo.findAllByRenterIdOrLenderId(userId, pageable)
}
