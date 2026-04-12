package com.rental.commerce.infrastructure.notification

import com.querydsl.jpa.impl.JPAQueryFactory
import com.rental.commerce.domain.common.PageQuery
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationRepository
import com.rental.commerce.domain.notification.QNotification
import jakarta.transaction.Transactional
import kotlin.math.ceil
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

    override fun findByUserId(userId: Long, pageQuery: PageQuery): PageResult<Notification> {
        val content = queryFactory
            .selectFrom(notification)
            .where(
                notification.userId.eq(userId),
                notification.deletedAt.isNull,
            )
            .orderBy(notification.createdAt.desc())
            .offset((pageQuery.page * pageQuery.size).toLong())
            .limit(pageQuery.size.toLong())
            .fetch()

        val total = queryFactory
            .select(notification.count())
            .from(notification)
            .where(
                notification.userId.eq(userId),
                notification.deletedAt.isNull,
            )
            .fetchOne() ?: 0L

        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / pageQuery.size).toInt()

        return PageResult(
            content = content,
            totalElements = total,
            totalPages = totalPages,
        )
    }

    override fun findById(notificationId: Long): Notification? {
        return queryFactory
            .selectFrom(notification)
            .where(
                notification.id.eq(notificationId),
                notification.deletedAt.isNull,
            )
            .fetchOne()
    }

    override fun countUnreadByUserId(userId: Long): Long {
        return queryFactory
            .select(notification.count())
            .from(notification)
            .where(
                notification.userId.eq(userId),
                notification.isRead.isFalse,
                notification.deletedAt.isNull,
            )
            .fetchOne() ?: 0L
    }

    @Transactional
    override fun markAllAsReadByUserId(userId: Long) {
        queryFactory
            .update(notification)
            .set(notification.isRead, true)
            .where(
                notification.userId.eq(userId),
                notification.isRead.isFalse,
                notification.deletedAt.isNull,
            )
            .execute()
    }
}
