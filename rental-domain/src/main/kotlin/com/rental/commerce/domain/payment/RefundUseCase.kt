package com.rental.commerce.domain.payment

fun interface RefundUseCase {
    fun execute(command: RefundCommand): Long
}

data class RefundCommand(val paymentId: Long)