# Sprint 4 QA 보고서 — BE

- **Date**: 2026-04-23
- **Sprint**: 4 — 분쟁/환불/검색/위시리스트/알림 설정
- **Status**: BE PASS (12 PR 머지), FE 진행 중 (레이트 리밋으로 지연)

---

## 1. 검증 범위

| 도메인 | BE PR | 상태 |
|--------|-------|------|
| DEVOPS-415 Flyway V18~V23 | #96 | ✅ merged |
| BE-401 Dispute 도메인 | #97 | ✅ merged |
| BE-402 Dispute 당사자 API | #98 | ✅ merged |
| BE-403 Admin Dispute API | #100 | ✅ merged (보안 hotfix 후속 — #111) |
| BE-404 Refund 도메인 + Port-Adapter | #101 | ✅ merged |
| BE-405 DisputeResolvedRefundListener | #104 | ✅ merged → refactor #112 |
| BE-406 RefundCompletedSettlementListener | #109 | ✅ merged |
| BE-410 Product 검색 확장 | #105 | ✅ merged |
| BE-411 비정규화 컬럼 리스너 | #110 | ✅ merged |
| BE-420/421 Wishlist + debounce | #107 | ✅ merged |
| BE-430/431 NotificationPreference + Dispatcher + ArchUnit | #108 | ✅ merged |
| hotfix: harness JSON + 보안 + 룰 위반 | #111 | ✅ merged |
| refactor: BE-405 Listener → DomainService | #112 | ✅ merged |

**총 머지 PR: 13개** (문서 3개 + BE 구현 11개 + hotfix 1개 + refactor 1개)

| FE 티켓 | PR | 상태 |
|---------|----|------|
| FE-450/451 분쟁 오픈·중재 UI | — | 레이트 리밋 대기 (3am KST 리셋 후) |
| FE-452 검색 필터/정렬 UI | — | 동일 |
| FE-453 위시리스트 UI | — | 동일 |
| FE-454 알림 설정 UI | — | 동일 |

---

## 2. 하네스 훅 상태 (중간 점검 결과 반영)

### 2.1 심각 이슈 발견 및 복구

**Sprint 4 BE 초기 ~ PR #110 기간 동안 하네스 훅이 silent pass 상태로 동작**했다.

- 원인: `.claude/harness-rules.json` line 710 테스트 요구사항 배열 객체 사이 **콤마 누락**으로 JSON 파싱 실패
- 영향: Write/Edit 에 대한 `code-pattern` 훅이 룰 로드 단계에서 예외 → exit 0 (silent pass)
- 조치: hotfix PR #111 에서 콤마 추가 + 실제 감지 검증 (`CLAUDE_TOOL_INPUT='...@Query...LocalDateTime...'` → `BLOCKED` exit 2)

### 2.2 훅 silent pass 기간의 2차 피해 및 복구

**harness-auditor 에이전트 전수 감사 결과**:

| 룰 ID | 발견 건수 | 조치 |
|-------|----------|------|
| no-double-bang | 2 (ResolveDisputeUseCase.kt:22,24) | #111 에서 requireNotNull 로 교체 |
| no-fqcn | 3 (Product.kt ratingAvg) + 1 (ReviewRepository.kt RatingSnapshot) | #111, #110 에서 import 추가 |
| no-kafka-topic-hardcode | 2 (Test 파일) | False positive — 이번 PR 에서 exclude_glob 확장 |
| no-cross-domain-import (admin/auth) | 2 | 룰 미커버 영역 — 이번 PR 에서 신규 룰 추가 |

### 2.3 pr-reviewer 에이전트 사후 리뷰 반영

- **PR #100 Critical**: `AdminDisputeApiController` `@RoleRequired("ADMIN")` 누락 (일반 사용자도 관리자 분쟁 액션 가능했던 **보안 취약점**) → #111 에서 수정
- **PR #98 Critical**: `GetDisputeUseCase` 에서 `dispute.openerId != requesterId` 직접 비교 → #111 에서 `Dispute.verifyAccessibleBy(requesterId, isAdmin)` 로 캡슐화
- **PR #104 Critical**: Listener 에 `RentalPaymentRepository` 직접 주입 + 이벤트 발행 → #112 refactor 로 `RefundDomainService.processRefundForRental()` 이동, Listener 는 얇은 분기만 유지

---

## 3. 아키텍처 체인 검증

### 분쟁 → 환불 → 정산 보정 체인 (ADR-009)

```
ResolveDisputeUseCase(@Transactional)
  └─ DisputeDomainService.resolve*
       └─ dispute.resolve* + publish(DisputeResolvedEvent)
  COMMIT
  └─ DisputeResolvedRefundListener  @TransactionalEventListener(AFTER_COMMIT) @Transactional(REQUIRES_NEW)
       └─ RefundDomainService.processRefundForRental(rentalId, disputeId, amount, reasonCode)
            ├─ rental_payment 조회
            ├─ processRefund: SELECT FOR UPDATE → Refund(PENDING) → PG 호출 → markSucceeded/Failed
            └─ publish(RefundCompletedEvent)
       COMMIT
       └─ RefundCompletedSettlementListener  @TransactionalEventListener(AFTER_COMMIT) @Transactional(REQUIRES_NEW)
            └─ SettlementDomainService.applyRefundAdjustment(rentalId, totalRefunded)
                 └─ Settlement.applyRefundAdjustment(totalRefunded) — idempotent
```

**핵심 특성**:
- 각 단계 독립 트랜잭션 — 한 단계 실패가 선행 단계 롤백하지 않음
- `RefundCompletedSettlementListener` 는 `sum(SUCCEEDED refunds by rental_id)` 로 매번 재계산 → 중복 이벤트에도 안전 (idempotent)
- 활성 분쟁 단일 제약: MySQL generated column + UNIQUE (V18)
- 부분환불 누적 금액 ≤ 원 결제 금액: 도메인 검증 + PENDING/SUCCEEDED 합산

---

## 4. 테스트 커버리지

### 4.1 BE Domain 단위 테스트 (Kotest BehaviorSpec)

| Entity/Service | 케이스 수 | 커버 포인트 |
|----------------|---------|-------------|
| DisputeStatus | 12 | 허용/비허용 전이 전수 |
| Dispute | 11 | create/startReview/resolve*/cancel + verifyAccessibleBy |
| Refund | 8 | 생성/상태 전이 엣지 |
| RefundStatus | (포함) | PENDING→SUCCEEDED/FAILED 단방향 |
| RefundDomainService | 4 | 초과/PG 성공/실패 응답/예외 |
| Settlement.applyRefundAdjustment | 5 | 환불 없음/부분/중복/초과/음수 |
| Wishlist / DomainService | 4 | add 중복·신규, remove 없음·정상 |
| NotificationPreference / Dispatcher | 4 | off/on/누락+rental/누락+marketing |
| ProductSearchCondition | 9 | page/size/price/신규 필드 |

### 4.2 Application 단위 테스트 (MockK)

| UseCase | 케이스 수 |
|---------|---------|
| OpenDisputeUseCase | 2 |
| GetDisputeUseCase | 3 (본인/타인 차단/관리자 통과) |
| CancelDisputeUseCase | 1 |
| StartDisputeReviewUseCase | 1 |
| ResolveDisputeUseCase | 4 (FULL_REFUND/PARTIAL/REJECTED/Command validation) |

### 4.3 Infrastructure 통합 테스트 (Testcontainers)

| 대상 | 케이스 수 |
|------|---------|
| FlywaySprint4MigrationTest | 7 (V18~V23 + UNIQUE/default) |
| DisputeResolvedRefundListenerTest (MockK) | 5 (refactor 후: REJECTED/CANCELLED/null/PARTIAL/FULL_REFUND) |
| RefundCompletedSettlementListenerTest (MockK) | 3 (FAILED 스킵/정상/Settlement 없음) |
| ProductDenormUpdater | 6 (리뷰 3 + 반납 3) |
| WishlistNotificationListener | 3 (available=false/debounce 혼합/서비스 예외) |

### 4.4 Presentation 통합 테스트

**갭 존재**: pr-reviewer 지적대로 `rental-api/src/test/.../dispute/`, `.../wishlist/`, `.../notification/` 디렉토리에 MockMvc 통합 테스트 부재. 후속 티켓 필요.

### 4.5 ArchUnit 경계 테스트

- `NotificationArchitectureTest` — `NotificationPort` 의존은 `NotificationDispatcher` + `..infrastructure.notification..` 에서만 허용

---

## 5. 보안 체크

### 5.1 수정된 이슈

- **Critical** `AdminDisputeApiController` @RoleRequired("ADMIN") 누락 → hotfix #111 반영
- 관리자 엔드포인트 전체 재점검: `AdminApiController`, `AdminDisputeApiController` 2개 ADMIN 보호 확인

### 5.2 확인된 방어

- 모든 `userId` 는 `@AuthenticatedMember` 에서만 주입 (body/쿼리 스푸핑 차단)
- 분쟁 조회 권한 검증은 `Dispute.verifyAccessibleBy()` 에 Rich Domain Model 로 캡슐화
- 위시리스트 CRUD 는 `userId` 를 쿠키/바디에서 받지 않음
- Toss PG 호출은 `@Profile("prod","staging")` 에서만 — 로컬/테스트는 Mock
- FK 미사용, DB ENUM/JSON/BOOLEAN/DATETIME 정밀도 준수

### 5.3 잔존 리스크

- 환불 PG API 실패 자동 재시도 없음 (MVP 설계상 수동 보정, ADR-009 §5 기록)
- 위시리스트 debounce In-Memory (다중 노드 시 Redis 교체 필요 — 운영 전환 전 티켓)

---

## 6. 하네스 룰 강화 (이 PR 포함)

harness-auditor 제안을 수용해 룰셋 3건 보강:

| 룰 ID | 변경 |
|-------|------|
| no-kafka-topic-hardcode | exclude_glob 에 `**/*Test*.kt` 추가 (테스트 string literal false positive 제거) |
| no-cross-domain-import-admin | **신규** — `domain/admin` 에서 다른 도메인 import 금지 (warning) |
| no-cross-domain-import-auth | **신규** — `domain/auth` 에서 user/rental/product/notification import 금지 (warning) |

`AdminResult.kt`, `AdminQueryRepository.kt` (→ `RentalStatus` 참조), `AuthDomainService.kt` (→ `SocialProvider` 참조) 가 현재 warning 으로 집계되며 후속 리팩토링 티켓 대상.

---

## 7. 추가 후속 티켓 제안

| 티켓 초안 | 근거 |
|-----------|------|
| RC-BE-441 Presentation 통합 테스트 — Dispute/Wishlist/NotificationPreference | pr-reviewer Major 지적 |
| RC-BE-442 BE-404/405 E2E 통합 테스트 (Testcontainers) | AFTER_COMMIT + REQUIRES_NEW 실제 스프링 컨텍스트 검증 |
| RC-BE-443 RefundDomainService @Transactional 명시 | pr-reviewer Major |
| RC-BE-444 RefundExceedsPaymentException 전용 ErrorCode | pr-reviewer Minor — 현재 INVALID_INPUT 재활용 |
| RC-BE-445 SocialProvider/RentalStatus 를 domain.common 으로 이동 | harness-auditor cross-domain 위반 |
| RC-DEVOPS-446 RedisNotificationDebouncer adapter | 다중 노드 운영 준비 |

---

## 8. 결론

Sprint 4 BE 13개 PR 머지 완료. 핵심 체인(분쟁→환불→정산 보정) 구현 + 검색 고도화 + 위시리스트 + NotificationDispatcher 강제 + ArchUnit 경계 검증 모두 통과.

중간 점검에서 발견된 **하네스 훅 silent pass 이슈는 hotfix 로 복구** 완료, silent 기간 동안 누락된 룰 위반(@RoleRequired, !!, FQCN, Listener 레이어 위반)은 사후 pr-reviewer/harness-auditor 감사로 전수 추적 후 #111/#112 에서 수정.

FE-450~454 는 에이전트 레이트 리밋(3am KST 리셋)으로 지연 중 — 리셋 후 재스폰 예정.
