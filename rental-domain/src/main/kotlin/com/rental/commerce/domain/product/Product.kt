package com.rental.commerce.domain.product

import com.rental.commerce.domain.common.BaseEntity
import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.DomainEvent
import com.rental.commerce.domain.common.ErrorCode
import com.rental.commerce.domain.common.InvalidStateTransitionException
import com.rental.commerce.domain.product.event.ProductApprovedEvent
import com.rental.commerce.domain.product.event.ProductRejectedEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient

@Entity
@Table(name = "product")
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    val productId: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "name", length = 100)
    var name: String? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "category_code", length = 50)
    var categoryCode: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "`condition`", length = 20)
    var condition: ProductCondition? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ProductStatus = ProductStatus.DRAFT,

    @Column(name = "current_draft_step")
    var currentDraftStep: Int? = null,

    @Column(name = "deposit_amount")
    var depositAmount: Long? = null,

    @Column(name = "reject_reason", length = 500)
    var rejectReason: String? = null,

) : BaseEntity() {

    @Transient
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()

    fun pullEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }

    fun updateDraft(
        step: Int,
        name: String?,
        description: String?,
        categoryCode: String?,
        condition: ProductCondition?,
        depositAmount: Long?,
    ) {
        if (status != ProductStatus.DRAFT) {
            throw InvalidStateTransitionException("DRAFT 상태에서만 임시저장을 업데이트할 수 있습니다")
        }

        this.currentDraftStep = step
        this.name = name
        this.description = description
        this.categoryCode = categoryCode
        this.condition = condition
        this.depositAmount = depositAmount
    }

    fun submit() {
        validateTransition(ProductStatus.UNDER_REVIEW)
        validateRequiredFieldsForSubmit()

        this.status = ProductStatus.UNDER_REVIEW
        this.currentDraftStep = null
    }

    fun approve() {
        validateTransition(ProductStatus.APPROVED)

        this.status = ProductStatus.APPROVED
        domainEvents.add(
            ProductApprovedEvent(
                productId = productId,
                userId = userId,
            )
        )
    }

    fun reject(reason: String) {
        validateTransition(ProductStatus.REJECTED)

        this.status = ProductStatus.REJECTED
        this.rejectReason = reason
        domainEvents.add(
            ProductRejectedEvent(
                productId = productId,
                userId = userId,
                reason = reason,
            )
        )
    }

    fun makeAvailable() {
        validateTransition(ProductStatus.AVAILABLE)

        this.status = ProductStatus.AVAILABLE
    }

    fun revertToDraft() {
        validateTransition(ProductStatus.DRAFT)

        this.status = ProductStatus.DRAFT
        this.rejectReason = null
    }

    private fun validateTransition(target: ProductStatus) {
        if (!status.canTransitTo(target)) {
            throw InvalidStateTransitionException(
                "${status.name}에서 ${target.name}(으)로 전이할 수 없습니다"
            )
        }
    }

    private fun validateRequiredFieldsForSubmit() {
        if (name.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "상품명은 필수입니다")
        }
        if (description.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "상품 설명은 필수입니다")
        }
        if (categoryCode.isNullOrBlank()) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "카테고리 코드는 필수입니다")
        }
        if (condition == null) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "상품 상태는 필수입니다")
        }
        if (depositAmount == null) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "보증금은 필수입니다")
        }
    }
}
