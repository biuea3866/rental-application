package com.rental.commerce.domain.wishlist.port

/**
 * 위시리스트 가용 알림 발송 Port (BE-421).
 *
 * 실제 발송 책임은 NotificationDispatcher(BE-431) 가 가지며, 그 앞의 얇은 어댑터가 이 Port 를 구현해 연결.
 * 본 Port 는 위시리스트 도메인이 NotificationDispatcher 를 직접 의존하지 않도록 하는 경계 역할.
 */
interface WishlistNotifier {
    fun notifyProductAvailable(userId: Long, productId: Long)
}
