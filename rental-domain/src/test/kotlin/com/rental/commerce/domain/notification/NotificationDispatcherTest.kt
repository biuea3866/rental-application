package com.rental.commerce.domain.notification

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class NotificationDispatcherTest : BehaviorSpec({

    val preferenceRepository = mockk<NotificationPreferenceRepository>()
    val notificationPort = mockk<NotificationPort>()
    val dispatcher = NotificationDispatcher(preferenceRepository, notificationPort)

    beforeEach { clearMocks(preferenceRepository, notificationPort) }

    Given("send") {
        fun pref(
            chat: Boolean = true, rental: Boolean = true,
            settlement: Boolean = true, marketing: Boolean = false,
        ): NotificationPreference {
            val p = NotificationPreference.createDefault(userId = 1L)
            p.update(chat, rental, settlement, marketing)
            return p
        }

        When("preference off (marketing=false 가 기본)") {
            Then("port 호출 안 됨") {
                every { preferenceRepository.findByUserId(1L) } returns pref()
                every { notificationPort.dispatch(any(), any(), any()) } just Runs

                dispatcher.send(
                    userId = 1L,
                    channel = NotificationChannel.MARKETING,
                    payload = NotificationPayload("t1"),
                )
                verify(exactly = 0) { notificationPort.dispatch(any(), any(), any()) }
            }
        }

        When("preference on") {
            Then("port.dispatch 호출") {
                every { preferenceRepository.findByUserId(2L) } returns pref(chat = true)
                every { notificationPort.dispatch(any(), any(), any()) } just Runs

                dispatcher.send(
                    userId = 2L,
                    channel = NotificationChannel.CHAT,
                    payload = NotificationPayload("t2"),
                )
                verify(exactly = 1) {
                    notificationPort.dispatch(2L, NotificationChannel.CHAT, any())
                }
            }
        }

        When("preference 존재하지 않음 (신규 유저)") {
            Then("안전 default 로 진행 — chat/rental/settlement 는 ON 처리") {
                every { preferenceRepository.findByUserId(3L) } returns null
                every { notificationPort.dispatch(any(), any(), any()) } just Runs

                dispatcher.send(
                    userId = 3L,
                    channel = NotificationChannel.RENTAL,
                    payload = NotificationPayload("t3"),
                )
                verify(exactly = 1) {
                    notificationPort.dispatch(3L, NotificationChannel.RENTAL, any())
                }
            }
        }

        When("preference 존재하지 않음 + marketing") {
            Then("기본값 OFF 이므로 port 호출 안 됨") {
                every { preferenceRepository.findByUserId(4L) } returns null

                dispatcher.send(
                    userId = 4L,
                    channel = NotificationChannel.MARKETING,
                    payload = NotificationPayload("t4"),
                )
                verify(exactly = 0) { notificationPort.dispatch(any(), any(), any()) }
            }
        }
    }
})
