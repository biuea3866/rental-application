package com.rental.commerce.infrastructure.notification

import com.rental.commerce.domain.notification.Notification
import com.rental.commerce.domain.notification.NotificationDomainService
import com.rental.commerce.domain.notification.NotificationType
import com.rental.commerce.domain.rental.RentalStatus
import com.rental.commerce.domain.rental.event.RentalStatusChangedEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZonedDateTime

class RentalStatusChangedEventWorkerTest : BehaviorSpec({

    given("RentalStatusChangedEventWorker — APPROVED 이벤트 수신") {

        `when`("REQUESTED → APPROVED 이벤트가 들어오면") {
            val notificationDomainService: NotificationDomainService = mockk()
            val worker = RentalStatusChangedEventWorker(notificationDomainService)
            val event = RentalStatusChangedEvent(
                rentalId = 1L,
                renterId = 10L,
                lenderId = 20L,
                fromStatus = RentalStatus.REQUESTED,
                toStatus = RentalStatus.APPROVED,
                occurredAt = ZonedDateTime.now(),
            )
            val notificationSlot = slot<Notification>()
            every { notificationDomainService.save(capture(notificationSlot)) } answers { firstArg() }

            worker.handle(event)

            then("renterId 에게 RENTAL_CONFIRMED 알림을 저장한다") {
                verify(exactly = 1) { notificationDomainService.save(any()) }
                val saved = notificationSlot.captured
                saved.userId shouldBe 10L
                saved.notificationType shouldBe NotificationType.RENTAL_CONFIRMED
                saved.referenceId shouldBe 1L
                saved.referenceType shouldBe "RENTAL"
            }
        }
    }

    given("RentalStatusChangedEventWorker — RETURNED 이벤트 수신") {

        `when`("IN_USE → RETURNED 이벤트가 들어오면") {
            val notificationDomainService: NotificationDomainService = mockk()
            val worker = RentalStatusChangedEventWorker(notificationDomainService)
            val event = RentalStatusChangedEvent(
                rentalId = 2L,
                renterId = 10L,
                lenderId = 20L,
                fromStatus = RentalStatus.IN_USE,
                toStatus = RentalStatus.RETURNED,
                occurredAt = ZonedDateTime.now(),
            )
            val notificationSlot = slot<Notification>()
            every { notificationDomainService.save(capture(notificationSlot)) } answers { firstArg() }

            worker.handle(event)

            then("lenderId 에게 RENTAL_COMPLETED 알림을 저장한다") {
                verify(exactly = 1) { notificationDomainService.save(any()) }
                val saved = notificationSlot.captured
                saved.userId shouldBe 20L
                saved.notificationType shouldBe NotificationType.RENTAL_COMPLETED
                saved.referenceId shouldBe 2L
            }
        }
    }

    given("RentalStatusChangedEventWorker — CANCELLED 이벤트 수신") {

        `when`("APPROVED → CANCELLED 이벤트가 들어오면") {
            val notificationDomainService: NotificationDomainService = mockk()
            val worker = RentalStatusChangedEventWorker(notificationDomainService)
            val event = RentalStatusChangedEvent(
                rentalId = 3L,
                renterId = 10L,
                lenderId = 20L,
                fromStatus = RentalStatus.APPROVED,
                toStatus = RentalStatus.CANCELLED,
                occurredAt = ZonedDateTime.now(),
            )
            val notificationSlot = slot<Notification>()
            every { notificationDomainService.save(capture(notificationSlot)) } answers { firstArg() }

            worker.handle(event)

            then("renterId 에게 RENTAL_CANCELLED 알림을 저장한다") {
                verify(exactly = 1) { notificationDomainService.save(any()) }
                val saved = notificationSlot.captured
                saved.userId shouldBe 10L
                saved.notificationType shouldBe NotificationType.RENTAL_CANCELLED
                saved.referenceId shouldBe 3L
            }
        }
    }

    given("RentalStatusChangedEventWorker — 알림 불필요 이벤트 수신") {

        `when`("REQUESTED → PAID 전이처럼 알림 불필요 상태 변경이면") {
            val notificationDomainService: NotificationDomainService = mockk(relaxed = true)
            val worker = RentalStatusChangedEventWorker(notificationDomainService)
            val event = RentalStatusChangedEvent(
                rentalId = 4L,
                renterId = 10L,
                lenderId = 20L,
                fromStatus = RentalStatus.REQUESTED,
                toStatus = RentalStatus.PAID,
                occurredAt = ZonedDateTime.now(),
            )

            worker.handle(event)

            then("알림을 저장하지 않는다") {
                verify(exactly = 0) { notificationDomainService.save(any()) }
            }
        }
    }
})
