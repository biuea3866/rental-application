package com.rental.commerce.domain.review.event

import com.rental.commerce.domain.common.DomainEvent
import java.time.ZonedDateTime

/**
 * 리뷰 생성 이벤트 — BE-411 ProductRatingUpdater 가 구독해 rating_avg/count 재계산.
 */
data class ReviewCreatedEvent(
    val reviewId: Long,
    val productId: Long,
    val renterId: Long,
    val rating: Int,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
