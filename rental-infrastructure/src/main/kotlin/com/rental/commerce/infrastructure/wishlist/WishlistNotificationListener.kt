package com.rental.commerce.infrastructure.wishlist

import com.rental.commerce.domain.wishlist.WishlistDomainService
import com.rental.commerce.domain.wishlist.event.ProductAvailabilityChangedEvent
import com.rental.commerce.domain.wishlist.port.NotificationDebouncer
import com.rental.commerce.domain.wishlist.port.WishlistNotifier
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * WishlistNotificationListener (BE-421, PRD-004 §4.4).
 *
 * ProductAvailabilityChangedEvent(available=true) 수신 시
 *  → 해당 상품을 위시리스트에 담은 사용자 전원에게 알림 발송
 *  → 사용자×상품 기준 24h debounce (InMemory/Redis 어댑터)
 *
 * 트랜잭션: AFTER_COMMIT + REQUIRES_NEW (본 레이스너의 실패가 원 트랜잭션을 롤백하지 않음)
 * 발송은 WishlistNotifier 포트 경유 — NotificationDispatcher(BE-431) 가 최종 책임.
 */
@Component
class WishlistNotificationListener(
    private val wishlistDomainService: WishlistDomainService,
    private val debouncer: NotificationDebouncer,
    private val notifier: WishlistNotifier,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAvailabilityChanged(event: ProductAvailabilityChangedEvent) {
        if (!event.available) return

        runCatching {
            val userIds = wishlistDomainService.findUserIdsWithProduct(event.productId)
            userIds.forEach { userId ->
                if (debouncer.tryAcquire(userId, event.productId, DEBOUNCE_TTL)) {
                    notifier.notifyProductAvailable(userId, event.productId)
                } else {
                    log.debug(
                        "[WishlistNotificationListener] debounce skip userId={} productId={}",
                        userId, event.productId,
                    )
                }
            }
        }.onFailure { ex ->
            log.error(
                "[WishlistNotificationListener] 처리 실패 productId={} error={}",
                event.productId, ex.message, ex,
            )
        }
    }

    companion object {
        private val DEBOUNCE_TTL: Duration = Duration.ofHours(24)
    }
}
