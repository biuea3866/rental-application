package com.rental.commerce.domain.refund.port

data class PaymentRefundResult(
    val success: Boolean,
    val externalRefundId: String?,
    val failureReason: String?,
) {
    companion object {
        fun succeeded(externalRefundId: String) =
            PaymentRefundResult(success = true, externalRefundId = externalRefundId, failureReason = null)

        fun failed(reason: String) =
            PaymentRefundResult(success = false, externalRefundId = null, failureReason = reason)
    }
}
