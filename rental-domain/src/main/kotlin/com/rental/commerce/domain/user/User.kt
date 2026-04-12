package com.rental.commerce.domain.user

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.PasswordHasher
import com.rental.commerce.domain.common.UnauthorizedException
import com.rental.commerce.domain.common.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user")
class User(
    @Column(nullable = false, length = 100, unique = true)
    val email: String,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false, length = 20)
    var phone: String,

    @Column(nullable = false, length = 255)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val socialProvider: SocialProvider? = null,

    @Column(length = 255)
    val socialProviderId: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Long? = null,
) : BaseEntity() {

    fun addRole(newRole: UserRole) {
        if (role == UserRole.BOTH || role == newRole) {
            return
        }
        if (newRole == UserRole.BOTH) {
            role = UserRole.BOTH
            return
        }
        // RENTER + LENDER = BOTH, LENDER + RENTER = BOTH
        if (role != newRole) {
            role = UserRole.BOTH
        }
    }

    fun updateProfile(name: String?, phone: String?) {
        name?.let { this.name = it }
        phone?.let { this.phone = it }
    }

    fun verifyPassword(rawPassword: String, passwordHasher: PasswordHasher) {
        if (!passwordHasher.matches(rawPassword, passwordHash)) {
            throw UnauthorizedException(
                errorCode = ErrorCode.INVALID_PASSWORD,
            )
        }
    }

    fun isSocialUser(): Boolean = socialProvider != null

    companion object {
        fun register(
            email: String,
            name: String,
            phone: String,
            passwordHash: String,
            role: UserRole,
        ): User = User(
            email = email,
            name = name,
            phone = phone,
            passwordHash = passwordHash,
            role = role,
        )
    }
}
