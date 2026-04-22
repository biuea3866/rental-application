package com.rental.commerce.domain.wishlist.event

import com.rental.commerce.domain.common.DomainEvent
import java.time.ZonedDateTime

/**
 * 상품 가용 상태 변경 이벤트 (PRD-004 §4.4 위시리스트 알림 트리거).
 *
 * AVAILABLE 으로 전이되는 순간 발행 — WishlistNotificationListener 가 구독.
 */
data class ProductAvailabilityChangedEvent(
    val productId: Long,
    val available: Boolean,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
