package com.rental.commerce.domain.payment

fun interface CancelUseCase {
    fun execute(command: CancelCommand): Long
}

data class CancelCommand(
    val paymentId: Long,
    val amount: Long
)