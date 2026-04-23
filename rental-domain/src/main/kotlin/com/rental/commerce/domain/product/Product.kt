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
import java.math.BigDecimal
import java.time.ZonedDateTime

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

    // Sprint 4 비정규화 컬럼 (V20). 리스너(BE-411)가 갱신.
    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    var ratingAvg: java.math.BigDecimal = java.math.BigDecimal.ZERO,

    @Column(name = "rating_count", nullable = false)
    var ratingCount: Int = 0,

    @Column(name = "rental_count", nullable = false)
    var rentalCount: Int = 0,

    @Column(name = "region_code", length = 20)
    var regionCode: String? = null,

    @Column(name = "base_price_amount")
    var basePriceAmount: Long? = null,

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = null,

) : BaseEntity() {

    @Transient
    // JPA/Hibernate는 리플렉션으로 엔티티를 로드할 때 @Transient 필드의
    // initializer를 실행하지 않아 null이 될 수 있음 (BLK-001).
    // private var + lazy getter 패턴으로 null-safe 보장.
    private var _domainEvents: MutableList<DomainEvent>? = null

    private val domainEvents: MutableList<DomainEvent>
        get() = _domainEvents ?: mutableListOf<DomainEvent>().also { _domainEvents = it }

    fun isPubliclyVisible(): Boolean {
        return status == ProductStatus.AVAILABLE || status == ProductStatus.RENTED
    }

    fun isOwnedBy(userId: Long): Boolean = this.userId == userId

    fun validateNotOwnedBy(userId: Long) {
        if (isOwnedBy(userId)) {
            throw BusinessException(
                errorCode = ErrorCode.FORBIDDEN,
                message = "자신의 상품은 대여 신청할 수 없습니다.",
            )
        }
    }

    fun validateAvailableForRental() {
        if (status != ProductStatus.AVAILABLE) {
            throw BusinessException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                message = "대여 신청 가능한 상품이 아닙니다 (현재 상태: $status)",
            )
        }
    }

    fun markAsRented() {
        validateTransition(ProductStatus.RENTED)
        this.status = ProductStatus.RENTED
    }

    fun pullEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }

    fun validateOwnership(requestUserId: Long) {
        if (userId != requestUserId) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_OWNERSHIP_DENIED,
                message = "해당 상품의 소유자가 아닙니다 (productId=$productId)",
            )
        }
    }

    fun validateDraftStatus() {
        if (status != ProductStatus.DRAFT) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_NOT_DRAFT,
                message = "임시저장 상태의 상품만 수정할 수 있습니다 (현재 상태: $status)",
            )
        }
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
        if (status != ProductStatus.UNDER_REVIEW) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_NOT_UNDER_REVIEW,
                message = "검수 중인 상품만 승인할 수 있습니다 (현재 상태: $status)",
            )
        }
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
        if (status != ProductStatus.UNDER_REVIEW) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_NOT_UNDER_REVIEW,
                message = "검수 중인 상품만 반려할 수 있습니다 (현재 상태: $status)",
            )
        }
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

    fun delete() {
        if (!status.canTransitTo(ProductStatus.DELETED)) {
            throw BusinessException(
                errorCode = ErrorCode.PRODUCT_NOT_DELETABLE,
                message = "삭제할 수 없는 상태의 상품입니다 (현재 상태: $status)",
            )
        }
        this.status = ProductStatus.DELETED
        this.deletedAt = ZonedDateTime.now()
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
