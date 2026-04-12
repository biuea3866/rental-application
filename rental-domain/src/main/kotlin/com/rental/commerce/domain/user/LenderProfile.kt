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
@Table(name = "lender_profile")
class LenderProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lender_profile_id")
    val lenderProfileId: Long = 0,

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "lender_type", nullable = false, length = 20)
    var lenderType: LenderType = LenderType.INDIVIDUAL,

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    var verificationStatus: VerificationStatus = VerificationStatus.PENDING,

    @Column(name = "settlement_account_bank", length = 50)
    var settlementAccountBank: String? = null,

    @Column(name = "settlement_account_number", length = 50)
    var settlementAccountNumber: String? = null,
) : BaseEntity() {

    fun registerSettlementAccount(bank: String, number: String) {
        this.settlementAccountBank = bank
        this.settlementAccountNumber = number
    }

    fun verify() {
        this.verificationStatus = VerificationStatus.VERIFIED
    }

    fun updateProfile(
        settlementAccountBank: String?,
        settlementAccountNumber: String?,
    ) {
        settlementAccountBank?.let { this.settlementAccountBank = it }
        settlementAccountNumber?.let { this.settlementAccountNumber = it }
    }
}
