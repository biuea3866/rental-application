package com.rental.commerce.domain.payment

fun interface ApprovalUseCase {
    fun execute(command: ApprovalCommand)
}

data class ApprovalCommand(
    val paymentId: Long,
    val amount: Long,
    val paymentType: PaymentType
)