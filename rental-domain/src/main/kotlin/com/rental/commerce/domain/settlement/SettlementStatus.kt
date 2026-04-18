package com.rental.commerce.domain.settlement

enum class SettlementStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
    ;

    fun validateCanComplete() {
        if (this != PENDING) {
            throw SettlementAlreadyProcessedException("이미 처리된 정산입니다. currentStatus=$this")
        }
    }
}
