# TDD-004: Sprint 4 — 분쟁/환불/검색/위시리스트/알림 기술 설계

- **Status**: Draft v1.0
- **Date**: 2026-04-22
- **Related PRD**: PRD-004-sprint4-dispute-refund-discovery.md
- **Related ADR**: ADR-008 (정산 자동화 이벤트 패턴), ADR-009 (분쟁 상태기계), ADR-010 (검색 엔진 선택)
- **Sprint**: 4

---

## Background

Sprint 1~3에서 회원/상품/대여/결제/리뷰/채팅/정산/관리자 핵심 플로우가 완성됐다. Sprint 4는 **"거래 실패 복구 + 탐색 경험 강화 + 재방문 유도"** 로, 기존 도메인의 경계를 침범하지 않고 5개 신규 도메인(dispute, refund, discovery, wishlist, notification-preference)을 추가한다.

도메인 간 결합은 ADR-008의 `@TransactionalEventListener(AFTER_COMMIT) + REQUIRES_NEW` 패턴으로 일원화한다.

---

## Terminology

| 용어 | 설명 |
|------|------|
| Dispute | 대여 거래 분쟁 Aggregate Root. 상태기계는 ADR-009 참조 |
| DisputeResolution | 분쟁 해결 결과 VO — FULL_REFUND / PARTIAL / REJECTED |
| Refund | 결제 환불 Entity. Payment와 1:N, 누적 금액은 원 결제 금액을 초과할 수 없음 |
| PaymentGatewayPort | 결제/환불 외부 API 추상화 (Port-Adapter, CLAUDE.md 규칙) |
| ProductSearchQuery | 검색 조건 VO — keyword/categoryIds/priceRange/regionCode/period/sort/page |
| ProductSearchRepository | QueryDSL 기반 검색 전용 리포지토리 |
| Wishlist | 사용자-상품 n:m 관계 Entity. UNIQUE(userId, productId) |
| NotificationPreference | 사용자별 채널(chat/rental/settlement/marketing) on/off 설정 |
| NotificationDispatcher | 수신 설정을 체크하고 발송하는 도메인 서비스 |

---

## Define Problem

1. **분쟁-환불-정산 3단계 비동기 체인**이 하나라도 실패해도 다른 단계가 훼손되지 않아야 함.
2. **환불 PG API 실패/지연**을 분쟁 결정 UX와 분리 (재시도 가능 상태로 관리).
3. **부분환불 누적 금액** ≤ 원 결제 금액 불변 보장 (경쟁 조건).
4. **검색 p95 < 500ms** — MySQL 단일 엔진 유지 (ADR-010), 인덱스·비정규화 컬럼으로 달성.
5. **위시리스트 알림 스팸 방지** — 동일 상품 `AVAILABLE` 이벤트 빈도 제어.
6. **알림 설정 체크 누락 방지** — 발송 경로 단일화 (`NotificationDispatcher` 경유 강제).

---

## Possible Solutions

### 분쟁 상태 & 활성 제약
| 방안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| MySQL generated column + UNIQUE | 단일 활성 분쟁을 DB 레벨 보장 | MySQL 8.0+ 전용 | ✅ 채택 (ADR-009) |
| 애플리케이션 락 | 구현 간단 | 다중 노드 취약 | 미채택 |
| Redis 분산락 | 강력 | 인프라 추가 | 미채택 (MVP) |

### 환불 처리 동기성
| 방안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| 분쟁 해결 트랜잭션 내 PG 동기 호출 | 즉시 일관성 | PG 장애 시 분쟁 결정 롤백 — 관리자 재조작 | 미채택 |
| AFTER_COMMIT 리스너에서 PG 호출 (REQUIRES_NEW) | 결정/환불 트랜잭션 분리, ADR-008과 동일 패턴 | 관리자 UX에 "pending" 상태 노출 필요 | ✅ 채택 |
| Outbox + 워커 재시도 | 재시도 자동화 | Sprint 4 범위 초과 | Sprint 5+ |

### 부분환불 누적 금액 보장
| 방안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| Payment 행 비관적 락(`SELECT … FOR UPDATE`) + 누적 검증 | 정합성 강함 | 행 경합 가능 | ✅ 채택 |
| DB CHECK 제약 (`sum(refund.amount) ≤ payment.amount`) | 선언적 | MySQL 8.0.16+ CHECK만 가능, 집계 CHECK는 제한 | 보조 방어 |
| 낙관적 락(version) | 경합 적음 | 충돌 시 재시도 루프 필요 | 미채택 |

### 검색 구현
| 방안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| QueryDSL 동적 쿼리 + 커버링 인덱스 | 추가 인프라 없음 | 키워드 풀텍스트 약함 | ✅ MVP (ADR-010) |
| Elasticsearch | 강력한 풀텍스트/Facet | 인덱싱 파이프라인·재색인 비용 | 트리거 충족 시 Sprint 6+ |

### 위시리스트 알림 debounce
| 방안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| 사용자×상품 기준 Redis TTL 24h | 단순, 노드 공유 | Redis 의존성 | ✅ 채택 |
| DB `last_notified_at` 컬럼 | 인프라 0 | 동시성 제어 번거로움 | 대안 |
| 배치 다이제스트 | 스팸 최소 | PRD-004 FR-4.3 요구는 즉시성 | 미채택 |

---

## Detail Design

### 1. 패키지 구조 (추가)

```
rental-domain/src/main/kotlin/com/rental/commerce/domain/
├── dispute/
│   ├── Dispute.kt                (Aggregate Root — 상태기계)
│   ├── DisputeStatus.kt          (enum + 전이 캡슐화)
│   ├── DisputeReason.kt
│   ├── DisputeDomainService.kt
│   ├── DisputeRepository.kt
│   └── event/
│       ├── DisputeCreatedEvent.kt
│       └── DisputeResolvedEvent.kt
├── refund/
│   ├── Refund.kt                 (Payment의 child)
│   ├── RefundStatus.kt           (PENDING / SUCCEEDED / FAILED)
│   ├── RefundDomainService.kt
│   └── port/
│       └── PaymentGatewayPort.kt (Port — CLAUDE.md 외부 API 규칙)
├── wishlist/
│   ├── Wishlist.kt
│   ├── WishlistDomainService.kt
│   └── WishlistRepository.kt
├── notification/
│   ├── NotificationPreference.kt
│   ├── NotificationChannel.kt    (CHAT / RENTAL / SETTLEMENT / MARKETING)
│   ├── NotificationDispatcher.kt (도메인 서비스 — 설정 체크 + 발송)
│   └── NotificationPreferenceRepository.kt
└── discovery/
    ├── ProductSearchQuery.kt     (VO)
    ├── ProductSearchResult.kt    (VO)
    └── ProductSearchRepository.kt (SPI)
```

### 2. 핵심 상태 전이 (ADR-009 요약)

```kotlin
// DisputeStatus.kt
enum class DisputeStatus {
    OPEN, UNDER_REVIEW,
    RESOLVED_REFUND, RESOLVED_PARTIAL, RESOLVED_REJECTED, CANCELLED;

    fun validateCanStartReview() { require(this == OPEN) { "Only OPEN can start review" } }
    fun validateCanResolve()     { require(this == UNDER_REVIEW) { "Resolve requires UNDER_REVIEW" } }
    fun validateCanCancel()      { require(this == OPEN) { "Only OPEN can be cancelled" } }
}
```

### 3. 분쟁→환불→정산 이벤트 체인

```
ResolveDisputeUseCase (@Transactional)
  └─ DisputeDomainService.resolve(dispute, resolution)
       ├─ dispute.resolvePartial(amount)   // 상태기계 검증 + 변경
       └─ publish(DisputeResolvedEvent)
  COMMIT
  ↓
RefundProcessingListener
  @TransactionalEventListener(AFTER_COMMIT)
  @Transactional(propagation = REQUIRES_NEW)
  └─ RefundDomainService.processRefund(rentalId, amount)
       ├─ Payment SELECT … FOR UPDATE
       ├─ 누적 금액 검증 (sum(refund.amount) + amount ≤ payment.amount)
       ├─ Refund(status = PENDING) 저장
       ├─ PaymentGatewayPort.requestRefund(...)
       └─ refund.markSucceeded() OR markFailed()
       └─ publish(RefundCompletedEvent)
  ↓
SettlementAdjustmentListener
  @TransactionalEventListener(AFTER_COMMIT)
  @Transactional(propagation = REQUIRES_NEW)
  └─ SettlementDomainService.applyRefundAdjustment(rentalId, refundAmount)
       ├─ 기존 Settlement 찾아 net_amount 감산 (음수 금지)
       └─ Settlement 상태에 따라 Adjustment 레코드 추가 저장
```

### 4. Port-Adapter — 결제/환불 외부 API

CLAUDE.md 규칙 "외부 API는 Port-Adapter 패턴(Gateway+Factory) 필수" 준수.

```kotlin
// domain/refund/port/PaymentGatewayPort.kt
interface PaymentGatewayPort {
    fun requestRefund(paymentKey: String, amount: BigDecimal, reason: String): RefundGatewayResult
}

// infrastructure/refund/gateway/TossPaymentGatewayAdapter.kt (Sprint 4 기본 Adapter)
@Component
class TossPaymentGatewayAdapter(
    private val tossApiClient: TossApiClient,
) : PaymentGatewayPort { ... }

// 테스트용
class FakePaymentGatewayAdapter : PaymentGatewayPort { ... }
```

`RefundDomainService`는 `PaymentGatewayPort`에만 의존 → 테스트 대체 용이, 추후 다른 PG 추가 가능.

### 5. 검색 쿼리 (MySQL + QueryDSL)

```sql
-- product 인덱스 (ADR-010)
CREATE INDEX idx_product_search
  ON product (status, category_id, region_code, price);

-- 비정규화 컬럼 (평점/인기 정렬용)
ALTER TABLE product
  ADD COLUMN rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN rental_count INT NOT NULL DEFAULT 0;
```

```kotlin
// infrastructure/discovery/ProductSearchRepositoryImpl.kt
class ProductSearchRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ProductSearchRepository {

    override fun search(query: ProductSearchQuery): Page<ProductSearchResult> {
        val condition = BooleanBuilder()
            .and(product.status.eq(ProductStatus.AVAILABLE))
            .andOptional(query.categoryIds?.let { product.categoryId.`in`(it) })
            .andOptional(query.minPrice?.let { product.price.goe(it) })
            .andOptional(query.maxPrice?.let { product.price.loe(it) })
            .andOptional(query.regionCode?.let { product.regionCode.eq(it) })
            .andOptional(query.period?.let { availabilityOverlaps(it) })

        return queryFactory
            .selectFrom(product)
            .where(condition)
            .orderBy(resolveOrder(query.sort))
            .offset(query.page * query.size)
            .limit(query.size.toLong())
            .fetch()
            .let { Page(it, ...) }
    }
}
```

- `@Query` 금지 (CLAUDE.md)
- 커버링 인덱스 순서: `status → category_id → region_code → price`
- 기간 필터는 `product_availability` 테이블 EXISTS 서브쿼리

### 6. 비정규화 컬럼 업데이트

```
ReviewCreatedEvent  ── AFTER_COMMIT ──▶ ProductRatingUpdater
                                        product.rating_avg = avg(reviews)

RentalStatusChangedEvent(RETURNED)
                    ── AFTER_COMMIT ──▶ ProductRentalCountUpdater
                                        product.rental_count += 1
```

업데이트 실패는 재계산 가능하므로 로그 + 야간 배치 재계산 허용 (Sprint 5 보강).

### 7. 위시리스트 알림 debounce

```
ProductAvailabilityChangedEvent(productId, AVAILABLE)
  └─ WishlistNotificationListener
       └─ for each user in wishlist(productId):
            if Redis SETNX "wishlist-notify:{userId}:{productId}" TTL=24h:
                NotificationDispatcher.send(user, channel=RENTAL, template=WISHLIST_AVAILABLE)
            else: skip
```

Redis 미가용 환경에서는 DB `wishlist_notification_log(user_id, product_id, last_notified_at)` 대안. Sprint 4 MVP는 Redis가 이미 인프라에 있으므로 Redis 우선.

### 8. NotificationDispatcher 경유 강제

```kotlin
// domain/notification/NotificationDispatcher.kt
class NotificationDispatcher(
    private val preferenceRepository: NotificationPreferenceRepository,
    private val notificationPort: NotificationPort,
) {
    fun send(userId: Long, channel: NotificationChannel, template: NotificationTemplate) {
        val pref = preferenceRepository.findByUserId(userId)
        if (!pref.isEnabled(channel)) return
        notificationPort.dispatch(userId, template.render())
    }
}
```

모든 알림 진입점은 `NotificationDispatcher.send()`만 사용. 다른 레이어가 `NotificationPort`를 직접 주입받지 못하게 **패키지 visibility** + **ArchUnit 테스트**로 강제.

### 9. DB 마이그레이션 (Flyway 예정 번호)

| 버전 | 내용 |
|------|------|
| V16 | `dispute` 테이블 + generated column UNIQUE |
| V17 | `refund` 테이블 + payment FK + 누적 금액 CHECK (가능 범위) |
| V18 | `product.rating_avg`, `product.rental_count` 컬럼 + 백필 |
| V19 | `idx_product_search` 커버링 인덱스 |
| V20 | `wishlist` + UNIQUE(user_id, product_id) |
| V21 | `notification_preference` + 기본값 triggers/insert-default |

### 10. 테스트 전략 (TDD — CLAUDE.md 규칙 준수)

#### Domain 단위 (Kotest BehaviorSpec)
- `DisputeStatusTest`: 허용/비허용 전이 각 1건 이상
- `Dispute.resolve()`: UNDER_REVIEW가 아닐 때 예외
- `Refund.add()`: 누적 금액 초과 시 예외
- `ProductSearchQuery`: 잘못된 기간(from > to) 거부

#### Application 단위 (MockK)
- `ResolveDisputeUseCase`: `dispute.resolve` 1회 + 이벤트 발행 확인
- `CreateWishlistItemUseCase`: 중복 호출 시 409
- `NotificationDispatcher`: preference off 시 notificationPort 미호출

#### Infrastructure 통합 (Testcontainers — 필수, MockK 단독 불가)
- `DisputeRepositoryImpl`: 활성 UNIQUE 위반 실제 발생
- `RefundRepositoryImpl`: `SELECT FOR UPDATE` 동시성 — 2스레드에서 누적 검증이 1건만 성공
- `ProductSearchRepositoryImpl`: 필터 조합 5개 케이스 + EXPLAIN으로 `idx_product_search` 사용 확인
- `WishlistRepositoryImpl`: UNIQUE 위반

#### Presentation 통합 (MockMvc + Testcontainers)
- `DisputeApiController` POST/PATCH 엔드포인트
- `AdminDisputeApiController` 해결 엔드포인트
- `ProductSearchApiController` 필터/정렬 5종
- `WishlistApiController` CRUD
- `NotificationPreferenceApiController` 조회/수정

#### 부하 테스트 (k6 — PRD-004 NFR)
- 검색 QPS 100 스파이크 — p95 < 500ms 회귀 시트
- 위시리스트 동시 추가 50 — UNIQUE 위반 정상 응답

#### 커버리지 목표
- FE/BE 각 95% (CLAUDE.md / feedback_test_coverage_95)

---

## Security

- **분쟁 첨부 이미지**: S3 presigned URL + 당사자/관리자 체크 (`DisputeAccessPolicy`)
- **환불 API**: ADMIN 권한만 호출 가능 (ADR-005 X-Member-Role)
- **위시리스트 사용자 ID**: `@AuthenticatedMember` 에서만 주입, 요청 바디의 userId는 신뢰하지 않음
- **알림 설정 변경**: 본인 확인 + CSRF/Rate-limit (기존 gateway 규칙 재사용)

---

## TDD Cycle 가이드

1. Red — `DisputeStatusTest` 작성, OPEN→RESOLVED_REFUND 불허 검증 실패
2. Green — `validateCanResolve()` 구현
3. Red — `ResolveDisputeUseCase` 통합 테스트 작성 (RefundListener까지)
4. Green — 리스너 + REQUIRES_NEW 구성
5. Refactor — 공통 이벤트 인터페이스 추출
6. 각 티켓은 "테스트 먼저 작성" + `feedback_tdd_first` 준수

---

## Open Questions (구현 킥오프 전 해결)

1. 환불 PG 호출 실패 시 재시도 횟수 기본값? (현재 0 — 수동 재처리. 정책 확정 필요)
2. 위시리스트 debounce 기간 24h가 사용자 UX에 충분한가? — Growth 팀 A/B
3. 비정규화 `rating_avg`를 REAL_TIME 업데이트 vs 배치 허용? — 리뷰 트래픽 추정 후 결정
4. MarketingChannel 기본값 OFF가 기존 가입자에게도 소급 적용? — 법무/PO

---

## References
- PRD-004
- ADR-008 (AFTER_COMMIT + REQUIRES_NEW 원형)
- ADR-009 (분쟁 상태기계)
- ADR-010 (검색 엔진 선택)
- CLAUDE.md — TDD-first, Port-Adapter, QueryDSL, Transactional 위치 규칙
