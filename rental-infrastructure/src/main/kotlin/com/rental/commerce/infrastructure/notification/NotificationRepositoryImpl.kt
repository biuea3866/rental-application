package com.rental.commerce.infrastructure.notification

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationRepository
import com.rental.commerce.domain.notification.QNotification
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class NotificationRepositoryImpl(
    private val notificationJpaRepository: NotificationJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : NotificationRepository {

    private val notification = QNotification.notification

    override fun save(notification: Notification): Notification {
        return notificationJpaRepository.save(notification)
    }

    override fun findByUserId(userId: Long, pageable: Pageable): Page<Notification> {
        val content = queryFactory
            .selectFrom(notification)
            .where(notification.userId.eq(userId))
            .orderBy(notification.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(notification.count())
            .from(notification)
            .where(notification.userId.eq(userId))
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    override fun findById(notificationId: Long): Notification? {
        return queryFactory
            .selectFrom(notification)
            .where(notification.notificationId.eq(notificationId))
            .fetchOne()
    }

    override fun countUnreadByUserId(userId: Long): Long {
        return queryFactory
            .select(notification.count())
            .from(notification)
            .where(
                notification.userId.eq(userId),
                notification.isRead.isFalse,
            )
            .fetchOne() ?: 0L
    }

    override fun markAllAsReadByUserId(userId: Long) {
        queryFactory
            .update(notification)
            .set(notification.isRead, true)
            .where(
                notification.userId.eq(userId),
                notification.isRead.isFalse,
            )
            .execute()
    }
}
