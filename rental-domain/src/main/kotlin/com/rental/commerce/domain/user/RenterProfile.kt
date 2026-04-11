package com.rental.commerce.domain.user

import com.rental.commerce.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "renter_profile")
class RenterProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renter_profile_id")
    val renterProfileId: Long = 0,

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_grade", nullable = false, length = 20)
    var trustGrade: TrustGrade = TrustGrade.BRONZE,

    @Column(name = "total_transaction_count", nullable = false)
    var totalTransactionCount: Int = 0,
) : BaseEntity() {

    fun incrementTransactionCount() {
        this.totalTransactionCount++
    }

    fun upgradeTrustGrade() {
        when {
            totalTransactionCount >= 50 && trustGrade == TrustGrade.SILVER -> {
                trustGrade = TrustGrade.GOLD
            }
            totalTransactionCount >= 10 && trustGrade == TrustGrade.BRONZE -> {
                trustGrade = TrustGrade.SILVER
            }
        }
    }
}
