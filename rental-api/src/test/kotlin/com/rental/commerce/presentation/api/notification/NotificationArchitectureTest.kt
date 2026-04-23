package com.rental.commerce.presentation.api.notification

import com.rental.commerce.domain.notification.NotificationDispatcher
import com.rental.commerce.domain.notification.NotificationPort
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.BehaviorSpec

/**
 * BE-431 — NotificationPort 필드 주입 차단 ArchUnit 검증.
 *
 * 원칙: 모든 알림 경로는 NotificationDispatcher 경유.
 *
 * 허용:
 *  - NotificationDispatcher 는 NotificationPort 를 필드로 소유 (정상 경로)
 *  - ..domain.notification.. 패키지는 Port 정의 / Payload / Channel 참조
 *  - ..infrastructure.notification.. 의 Port 구현체(Adapter) 는 implements 허용 (Spring Bean 제공)
 *
 * 금지:
 *  - 위 외의 클래스가 NotificationPort 를 직접 참조(필드 주입 / 메서드 호출) 하는 것
 */
class NotificationArchitectureTest : BehaviorSpec({

    val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.rental.commerce")

    Given("NotificationPort 직접 주입 제약") {
        Then("NotificationPort 를 참조하는 클래스는 NotificationDispatcher / infrastructure.notification.. 뿐이어야 한다") {
            val rule = noClasses()
                .that().doNotHaveFullyQualifiedName(NotificationDispatcher::class.java.name)
                .and().resideOutsideOfPackage("..domain.notification..")
                .and().resideOutsideOfPackage("..infrastructure.notification..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(NotificationPort::class.java.name)
                .because(
                    "NotificationPort 직접 주입 금지 — 사용자별 preference 체크를 위해 NotificationDispatcher 경유 필수. " +
                        "Port 구현체(Adapter)는 infrastructure.notification 패키지에서만 허용.",
                )
            rule.check(classes)
        }
    }
})
