package com.rental.commerce.application.dispute

import java.math.BigDecimal

/**
 * 분쟁 해결 커맨드.
 *
 * - FULL_REFUND: 전체 환불. refundAmount 필수.
 * - PARTIAL:     부분 환불. refundAmount 필수.
 * - REJECTED:    기각. refundAmount 는 무시.
 */
data class ResolveDisputeCommand(
    val disputeId: Long,
    val type: ResolutionType,
    val refundAmount: BigDecimal? = null,
) {
    init {
        when (type) {
            ResolutionType.FULL_REFUND, ResolutionType.PARTIAL ->
                require(refundAmount != null && refundAmount > BigDecimal.ZERO) {
                    "${type.name} 해결 시 refundAmount 는 0보다 커야 합니다"
                }
            ResolutionType.REJECTED -> Unit
        }
    }

    enum class ResolutionType { FULL_REFUND, PARTIAL, REJECTED }
}
