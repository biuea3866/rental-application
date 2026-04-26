package com.rental.commerce.infrastructure.product

import com.rental.commerce.domain.product.ProductRepository
import com.rental.commerce.domain.rental.RentalRepository
import com.rental.commerce.domain.common.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import com.rental.commerce.domain.review.ReviewRepository
import com.rental.commerce.domain.review.event.ReviewCreatedEvent
import java.math.BigDecimal
import java.math.RoundingMode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * ProductDenormUpdater (BE-411, ADR-010 §3).
 *
 * Sprint 4 비정규화 컬럼 업데이트 리스너:
 *  - ReviewCreatedEvent → product.rating_avg / rating_count 재계산 (idempotent)
 *  - RentalStatusChangedEvent(RETURNED) → product.rental_count 증분
 *
 * 트랜잭션 경계:
 *  - @TransactionalEventListener(AFTER_COMMIT)
 *  - @Transactional(propagation = REQUIRES_NEW)
 *
 * 실패는 로그만 남기고 전파하지 않음 (ADR-010 — 실패 시 재계산 배치 허용).
 */
@Component
class ProductDenormUpdater(
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
    private val rentalRepository: RentalRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onReviewCreated(event: ReviewCreatedEvent) {
        runCatching {
            val productId = event.productId
            val product = productRepository.findById(productId) ?: run {
                log.warn("[ProductDenormUpdater] product 없음 — 스킵. productId={}", productId)
                return
            }
            val snapshot = reviewRepository.calculateRatingSnapshot(productId)
            product.applyRatingSnapshot(
                newAvg = snapshot.average.setScale(2, RoundingMode.HALF_UP),
                newCount = snapshot.count,
            )
            productRepository.save(product)
        }.onFailure { ex ->
            log.error(
                "[ProductDenormUpdater.onReviewCreated] 갱신 실패 reviewId={} productId={} error={}",
                event.reviewId, event.productId, ex.message, ex,
            )
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onRentalReturned(event: RentalStatusChangedEvent) {
        if (event.toStatus != RentalStatus.RETURNED) return

        runCatching {
            val rental = rentalRepository.findById(event.rentalId) ?: run {
                log.warn("[ProductDenormUpdater] rental 없음 — 스킵. rentalId={}", event.rentalId)
                return
            }
            val product = productRepository.findById(rental.productId) ?: run {
                log.warn("[ProductDenormUpdater] product 없음 — 스킵. productId={}", rental.productId)
                return
            }
            product.incrementRentalCount()
            productRepository.save(product)
        }.onFailure { ex ->
            log.error(
                "[ProductDenormUpdater.onRentalReturned] 갱신 실패 rentalId={} error={}",
                event.rentalId, ex.message, ex,
            )
        }
    }
}
