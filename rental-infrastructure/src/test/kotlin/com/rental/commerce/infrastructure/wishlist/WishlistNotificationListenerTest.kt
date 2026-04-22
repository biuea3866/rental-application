package com.rental.commerce.infrastructure.wishlist

import com.rental.commerce.domain.wishlist.WishlistDomainService
import com.rental.commerce.domain.wishlist.event.ProductAvailabilityChangedEvent
import com.rental.commerce.domain.wishlist.port.NotificationDebouncer
import com.rental.commerce.domain.wishlist.port.WishlistNotifier
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import java.time.Duration

class WishlistNotificationListenerTest : BehaviorSpec({

    val service = mockk<WishlistDomainService>()
    val debouncer = mockk<NotificationDebouncer>()
    val notifier = mockk<WishlistNotifier>()
    val listener = WishlistNotificationListener(service, debouncer, notifier)

    beforeEach { clearMocks(service, debouncer, notifier) }

    Given("onAvailabilityChanged") {
        When("available=false") {
            Then("처리 스킵") {
                listener.onAvailabilityChanged(ProductAvailabilityChangedEvent(10L, available = false))
                verify(exactly = 0) { service.findUserIdsWithProduct(any()) }
            }
        }

        When("available=true + debounce 통과 유저 2명 / 스킵 1명") {
            Then("debounce 통과한 사용자만 notify") {
                every { service.findUserIdsWithProduct(100L) } returns listOf(1L, 2L, 3L)
                every { debouncer.tryAcquire(1L, 100L, any<Duration>()) } returns true
                every { debouncer.tryAcquire(2L, 100L, any<Duration>()) } returns false
                every { debouncer.tryAcquire(3L, 100L, any<Duration>()) } returns true
                every { notifier.notifyProductAvailable(any(), any()) } just Runs

                listener.onAvailabilityChanged(ProductAvailabilityChangedEvent(100L, available = true))

                verify(exactly = 1) { notifier.notifyProductAvailable(1L, 100L) }
                verify(exactly = 0) { notifier.notifyProductAvailable(2L, 100L) }
                verify(exactly = 1) { notifier.notifyProductAvailable(3L, 100L) }
            }
        }

        When("서비스 예외") {
            Then("로그만 남기고 전파 안 함") {
                every { service.findUserIdsWithProduct(999L) } throws RuntimeException("DB down")
                listener.onAvailabilityChanged(ProductAvailabilityChangedEvent(999L, available = true))
                verify(exactly = 0) { notifier.notifyProductAvailable(any(), any()) }
            }
        }
    }
})
