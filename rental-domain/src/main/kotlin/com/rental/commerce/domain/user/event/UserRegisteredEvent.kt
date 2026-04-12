package com.rental.commerce.domain.user.event

import com.rental.commerce.domain.common.DomainEvent
import com.rental.commerce.domain.user.UserRole
import java.time.ZonedDateTime

data class UserRegisteredEvent(
    val userId: Long,
    val email: String,
    val role: UserRole,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
