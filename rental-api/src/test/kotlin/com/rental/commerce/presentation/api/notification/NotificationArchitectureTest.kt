package com.rental.commerce.presentation.api.notification

import com.rental.commerce.domain.notification.NotificationDispatcher
import com.rental.commerce.domain.notification.NotificationPort
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.BehaviorSpec

/**
 * BE-431 — NotificationPort 직접 주입 차단 ArchUnit 검증.
 *
 * 원칙: 모든 알림 경로는 NotificationDispatcher 경유. NotificationPort 는 Dispatcher 만 주입받을 수 있다.
 */
class NotificationArchitectureTest : BehaviorSpec({

    val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.rental.commerce")

    Given("NotificationPort 주입 제약") {
        Then("NotificationPort 를 의존하는 클래스는 NotificationDispatcher 뿐이어야 한다") {
            val rule = noClasses()
                .that().doNotHaveFullyQualifiedName(NotificationDispatcher::class.java.name)
                // 도메인 내부 Port 정의/DTO 는 예외 허용
                .and().resideOutsideOfPackage("..domain.notification..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(NotificationPort::class.java.name)
                .because(
                    "NotificationPort 직접 주입 금지 — 사용자별 preference 체크를 위해 NotificationDispatcher 경유 필수",
                )
            rule.check(classes)
        }
    }
})
