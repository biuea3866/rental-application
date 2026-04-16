package com.rental.commerce.domain.rental

/**
 * 결제 외부 시스템 Port Interface.
 *
 * Domain 레이어에 정의된 Port — Infrastructure 레이어의 Adapter가 구현한다.
 * ADR-005 (결제 Gateway 추상화) 참고.
 */
interface PaymentGateway {

    /**
     * 결제 승인 요청.
     *
     * @param request 결제 승인에 필요한 orderId, amount, paymentKey 정보
     * @return 승인 결과 (success=false 시 failureMessage에 사유 포함)
     */
    fun requestPayment(request: PaymentApproveRequest): PaymentResult

    /**
     * 결제 취소 요청.
     *
     * @param paymentKey 취소할 결제의 외부 결제 키
     * @param reason 취소 사유
     * @return 취소 결과 (success=false 시 환불 미처리)
     */
    fun cancelPayment(paymentKey: String, reason: String): PaymentCancelResult
}
