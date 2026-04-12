package com.rental.commerce.infrastructure.payment.gateway

import com.rental.commerce.domain.payment.ApprovalCommand
import com.rental.commerce.domain.payment.CancelCommand
import com.rental.commerce.domain.payment.PaymentGateway
import com.rental.commerce.domain.payment.PaymentType
import com.rental.commerce.domain.payment.RefundCommand
import org.springframework.stereotype.Repository

@Repository
class NaverPayRepository(
    private val naverPayApiClient: NaverPayApiClient
): PaymentGateway {
    override val type: PaymentType
        get() = PaymentType.NAVER_PAY

    override fun approve(command: ApprovalCommand) {
        TODO("Not yet implemented")
    }

    override fun cancel(command: CancelCommand) {
        TODO("Not yet implemented")
    }

    override fun refund(command: RefundCommand) {
        TODO("Not yet implemented")
    }
}