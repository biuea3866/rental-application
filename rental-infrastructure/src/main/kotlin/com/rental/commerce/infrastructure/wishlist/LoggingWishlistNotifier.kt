package com.rental.commerce.infrastructure.wishlist

import com.rental.commerce.domain.wishlist.port.WishlistNotifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * WishlistNotifier 기본 어댑터 — 로그만 기록.
 *
 * 실제 발송(푸시/이메일)은 후속 티켓에서 NotificationDispatcher(BE-431) 경유로 교체.
 * 현재는 Listener 의 컨슈머 경로를 로컬/CI 에서 안전하게 실행하기 위한 스텁.
 */
@Component
class LoggingWishlistNotifier : WishlistNotifier {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun notifyProductAvailable(userId: Long, productId: Long) {
        log.info(
            "[LoggingWishlistNotifier] product available notify userId={} productId={}",
            userId, productId,
        )
    }
}
