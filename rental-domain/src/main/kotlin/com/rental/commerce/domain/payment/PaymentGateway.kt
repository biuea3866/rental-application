package com.rental.commerce.domain.payment

interface PaymentGateway {
    val type: PaymentType

    fun approve(command: ApprovalCommand)

    fun cancel(command: CancelCommand)

    fun refund(command: RefundCommand)
}

enum class PaymentType {
    WOORI_CARD,
    KB_CARD,
    TOSS_PAYMENT,
    DANAL,
    NAVER_PAY,
    KAKAO_PAY
}