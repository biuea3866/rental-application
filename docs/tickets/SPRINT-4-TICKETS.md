# Sprint 4 티켓 분해 — 분쟁/환불/검색/위시리스트/알림

- **Date**: 2026-04-22
- **Source**: PRD-004, ADR-009, ADR-010, TDD-004
- **기준**: "1명 / 1일(≤8h) / 1PR" — `claude-framework:ticket-breakdown` 규칙
- **브랜치 prefix**: `feature/RC-BE-4xx`, `feature/RC-FE-4xx`, `feature/RC-DEVOPS-4xx`

각 티켓은 **테스트 먼저 작성 → 구현 → 통합 테스트** 순서(TDD), Jira/Notion에 그대로 복사 가능한 형식.

---

## 의존성 그래프 (상위)

```
V16~V21 (DEVOPS-415 Flyway)
   ↓
BE-401 Dispute 도메인 ─── BE-402 Dispute UseCase/API ─── BE-403 Admin Dispute API
   ↓ DisputeResolvedEvent
BE-404 Refund Port/Adapter ── BE-405 RefundProcessingListener
   ↓ RefundCompletedEvent
BE-406 SettlementAdjustmentListener

BE-410 Product search (QueryDSL) ── BE-411 rating_avg/rental_count 비정규화 리스너

BE-420 Wishlist 도메인/API ── BE-421 WishlistNotificationListener (Redis debounce)

BE-430 NotificationPreference 도메인/API ── BE-431 NotificationDispatcher + ArchUnit

FE-450 분쟁 오픈/조회 UI
FE-451 관리자 분쟁 중재 UI
FE-452 검색 필터/정렬 UI
FE-453 위시리스트 UI
FE-454 알림 설정 UI

DEVOPS-460 k6 검색 부하 테스트
DEVOPS-461 관측 지표(환불 실패 알람 등)
```

---

## 티켓 목록

### DEVOPS-415 — Flyway V16~V21 마이그레이션
- **도메인**: DB
- **설명**: dispute/refund/product(평점,대여수 컬럼)/wishlist/notification_preference 스키마 + 인덱스 + MySQL generated column UNIQUE 추가.
- **Acceptance Criteria**
  - V16: `dispute` 테이블 + `active_rental_id` generated column + `uk_dispute_active_rental` UNIQUE
  - V17: `refund` 테이블 + `payment_id` FK
  - V18: `product.rating_avg`, `product.rental_count` 컬럼 + NOT NULL DEFAULT 0 + 기존 row 백필
  - V19: `idx_product_search (status, category_id, region_code, price)` 커버링 인덱스
  - V20: `wishlist(user_id, product_id)` UNIQUE
  - V21: `notification_preference` + 기본값(chat/rental/settlement=ON, marketing=OFF)
- **대응 TC**: Testcontainers 기동 + `SHOW INDEX` / `SHOW COLUMNS` 로 확인하는 infra smoke 테스트
- **종속성**: 없음 (최우선)
- **추정**: 0.5d
- **Open Questions**: 기존 user/member row에 `notification_preference` 백필 전략 확정

---

### BE-401 — Dispute 도메인 (Entity + Status 상태기계)
- **도메인**: dispute
- **설명**: `Dispute` Aggregate Root + `DisputeStatus` enum (상태 전이 캡슐화) + `DisputeReason` enum + 도메인 이벤트 클래스.
- **Acceptance Criteria**
  - `DisputeStatus.validateCanStartReview/Resolve/Cancel` 예외 케이스 테스트 통과
  - `Dispute.resolvePartial(amount)` 가 UNDER_REVIEW 외 상태에서 예외
  - `DisputeCreatedEvent`, `DisputeResolvedEvent` VO (unmodifiable)
- **대응 TC** (Kotest)
  - `DisputeStatusTest`: 허용 전이 6개 / 비허용 전이 6개
  - `DisputeTest`: create/cancel/resolve 각 happy + edge
- **종속성**: DEVOPS-415
- **추정**: 0.75d
- **Notes**: CLAUDE.md `enum에 상태 전이 규칙 캡슐화` 준수

---

### BE-402 — Dispute UseCase + API (당사자용)
- **도메인**: dispute
- **설명**: 대여자/등록자가 분쟁을 오픈/조회/취소하는 엔드포인트.
- **Acceptance Criteria**
  - `POST /api/v1/disputes` — rentalId + reason + description + 이미지 URL 목록 / 활성 분쟁 존재 시 409
  - `GET /api/v1/disputes/{id}` — 당사자 또는 관리자만 200, 그 외 403
  - `POST /api/v1/disputes/{id}/cancel` — opener 본인 + OPEN 상태만
- **대응 TC**
  - UseCase 단위: MockK로 DomainService 호출 1회 검증
  - Repository 통합: `DisputeRepositoryImpl` UNIQUE 위반 케이스
  - Controller 통합: MockMvc 3개 엔드포인트 happy + 409/403
- **종속성**: BE-401
- **추정**: 1d

---

### BE-403 — Admin Dispute API (중재 엔드포인트)
- **도메인**: dispute (admin)
- **설명**: 관리자가 `UNDER_REVIEW` 전이 및 `RESOLVED_REFUND/PARTIAL/REJECTED` 결정.
- **Acceptance Criteria**
  - `PATCH /api/v1/admin/disputes/{id}/review` — OPEN→UNDER_REVIEW
  - `POST /api/v1/admin/disputes/{id}/resolve` — type + (PARTIAL일 때) refundAmount
  - 해결 시 `DisputeResolvedEvent` 발행 확인
- **대응 TC**
  - UseCase: 이벤트 발행 캡처 (ApplicationEvents)
  - Controller 통합: ADMIN role 외 403
- **종속성**: BE-402
- **추정**: 1d

---

### BE-404 — Refund 도메인 + PaymentGatewayPort (Port-Adapter)
- **도메인**: refund
- **설명**: `Refund` Entity + `RefundStatus` + `PaymentGatewayPort` 인터페이스 + `TossPaymentGatewayAdapter`(실제) + `FakePaymentGatewayAdapter`(테스트).
- **Acceptance Criteria**
  - `Refund.add()` 누적 금액 초과 시 도메인 예외
  - `PaymentGatewayPort` 인터페이스만 domain/application에서 의존
  - Infrastructure에만 Toss 호출 로직 존재 (CLAUDE.md Port-Adapter 규칙)
- **대응 TC**
  - `RefundTest`: 누적 검증 경계값(초과/등호)
  - `TossPaymentGatewayAdapterTest`: WireMock 기반 200/4xx/5xx 케이스
  - `FakePaymentGatewayAdapter` 유틸이 통합 테스트에서 주입 가능
- **종속성**: DEVOPS-415
- **추정**: 1d
- **Notes**: CLAUDE.md — 외부 API는 Port-Adapter(Gateway+Factory) 필수

---

### BE-405 — RefundProcessingListener (AFTER_COMMIT + REQUIRES_NEW)
- **도메인**: refund
- **설명**: `DisputeResolvedEvent` 수신 → Payment 비관적 락 → Refund 저장 → PG 호출 → `RefundCompletedEvent` 발행.
- **Acceptance Criteria**
  - AFTER_COMMIT 후 실행 (분쟁 트랜잭션 커밋 이후)
  - Propagation.REQUIRES_NEW로 독립 트랜잭션
  - `SELECT … FOR UPDATE`로 누적 금액 검증 동시성 보장
  - PG 실패 시 `refund.status = FAILED` 저장 + 관리자 알림(BE-431 경유)
- **대응 TC**
  - Integration: Testcontainers + 2스레드 부분환불 동시 호출 → 1건만 성공
  - Integration: Fake PG `throws` 주입 시 분쟁 상태 유지 확인
- **종속성**: BE-403, BE-404
- **추정**: 1d
- **Notes**: ADR-008과 동일 패턴

---

### BE-406 — SettlementAdjustmentListener
- **도메인**: settlement
- **설명**: `RefundCompletedEvent` 수신 → 기존 Settlement net_amount 감산 또는 보정 레코드 생성.
- **Acceptance Criteria**
  - Settlement 아직 미생성이면 스킵 + 로그
  - 감산 후 net_amount < 0 이면 예외
  - 멱등: 동일 RefundCompletedEvent 중복 수신 시 1회만 반영 (refund_id 추적)
- **대응 TC**
  - `SettlementDomainServiceTest.applyRefundAdjustment()` happy + 음수 방지
  - Integration: 중복 이벤트 시 2번째는 skip 로그
- **종속성**: BE-405
- **추정**: 0.75d

---

### BE-410 — Product 검색 리포지토리 (QueryDSL)
- **도메인**: discovery
- **설명**: `ProductSearchQuery` VO + `ProductSearchRepositoryImpl`(QueryDSL) + 5가지 정렬.
- **Acceptance Criteria**
  - 필터 조합 5종 (카테고리 다중, 가격 범위, 지역, 기간, keyword LIKE) 동작
  - 정렬 5종 (최신/가격 asc/desc/평점/인기) 동작
  - EXPLAIN 결과에 `idx_product_search` 사용 확인 (테스트에서 assert)
- **대응 TC**
  - Testcontainers 통합: 필터 조합 5케이스 + 정렬 5케이스
  - EXPLAIN 기반 커버링 인덱스 사용 검증
- **종속성**: DEVOPS-415 (V18, V19)
- **추정**: 1d

---

### BE-411 — 비정규화 컬럼 리스너 (rating_avg, rental_count)
- **도메인**: discovery
- **설명**: `ReviewCreatedEvent` → `product.rating_avg` 재계산. `RentalStatusChangedEvent(RETURNED)` → `product.rental_count += 1`.
- **Acceptance Criteria**
  - AFTER_COMMIT + REQUIRES_NEW
  - 실패 시 로그 + 재계산 배치 명세 (Sprint 5로 미룸, 티켓만 작성)
- **대응 TC**
  - Integration: 리뷰 3개 생성 → avg 계산 정확
  - Integration: RETURNED 3번 → count 3
- **종속성**: BE-410
- **추정**: 0.75d

---

### BE-420 — Wishlist 도메인 + API
- **도메인**: wishlist
- **설명**: Wishlist Entity + UseCase (add/remove/list) + Controller.
- **Acceptance Criteria**
  - 중복 추가 시 409 (UNIQUE 제약)
  - `GET /api/v1/wishlist` 페이지네이션
  - 사용자 ID는 `@AuthenticatedMember` 에서만 주입, 바디의 userId는 무시
- **대응 TC**
  - 도메인: 중복 방지
  - Controller: happy + 409 + 401(비로그인)
- **종속성**: DEVOPS-415 (V20)
- **추정**: 0.75d

---

### BE-421 — WishlistNotificationListener (Redis debounce)
- **도메인**: wishlist
- **설명**: `ProductAvailabilityChangedEvent(AVAILABLE)` 수신 → 위시리스트 사용자에게 알림. Redis SETNX 24h debounce.
- **Acceptance Criteria**
  - 동일 user×product 24h 내 재알림 없음
  - 알림 발송은 `NotificationDispatcher.send(channel=RENTAL)` 경유 (BE-431)
- **대응 TC**
  - Integration: embedded Redis or Testcontainers Redis → 1st SEND, 2nd skip
  - channel 설정 off 시 발송 안 됨
- **종속성**: BE-420, BE-431
- **추정**: 1d

---

### BE-430 — NotificationPreference 도메인 + API
- **도메인**: notification
- **설명**: 사용자별 채널 on/off 저장 + 조회/수정 엔드포인트.
- **Acceptance Criteria**
  - 기본값: chat/rental/settlement=ON, marketing=OFF
  - `GET /api/v1/me/notification-preferences`
  - `PATCH /api/v1/me/notification-preferences` partial update
- **대응 TC**
  - Controller 통합: 본인만 접근 200, 타 유저 403
  - 기본값 적용 로직 (신규 유저)
- **종속성**: DEVOPS-415 (V21)
- **추정**: 0.75d

---

### BE-431 — NotificationDispatcher + ArchUnit 경계 강제
- **도메인**: notification
- **설명**: 모든 알림 진입점을 `NotificationDispatcher.send()` 경유 강제. ArchUnit 테스트로 `NotificationPort` 직접 주입 차단.
- **Acceptance Criteria**
  - `NotificationDispatcher` 경유하지 않는 호출 경로 0건 (ArchUnit 테스트 PASS)
  - preference off 시 발송 스킵 (단위 테스트)
- **대응 TC**
  - 단위: preference mocked off → port.dispatch 0회
  - ArchUnit: 도메인 외부에서 `NotificationPort` 주입 시 실패
- **종속성**: BE-430
- **추정**: 0.75d

---

### FE-450 — 분쟁 오픈/조회 UI
- **도메인**: fe / dispute
- **설명**: 마이페이지 > 대여 상세에서 분쟁 오픈 버튼 + 폼(사유/설명/이미지 업로드) + 분쟁 목록/상세.
- **Acceptance Criteria**
  - 이미지 presigned URL 업로드 (최대 5장, 10MB/장)
  - 409 에러 시 "이미 진행 중인 분쟁" 토스트
  - OPEN 상태에서만 "취소" 버튼 노출
- **대응 TC**
  - Vitest + MSW: 폼 검증 + 409 분기
  - 컴포넌트 테스트: 상태별 버튼 표시
- **종속성**: BE-402
- **추정**: 1d

---

### FE-451 — 관리자 분쟁 중재 UI
- **도메인**: fe / admin
- **설명**: 관리자 대시보드에 분쟁 목록 + 상세(증빙 이미지 갤러리) + "검토 시작 / 전체환불 / 부분환불 / 기각" 액션.
- **Acceptance Criteria**
  - 상태별 필터 + 날짜 정렬
  - 부분환불 액션 시 금액 입력 모달
  - 해결 완료 후 목록 재페치 + "pending PG" 뱃지 표시 (PG 완료 전까지)
- **대응 TC**
  - MSW: PATCH/POST 2개 엔드포인트 + 실패 케이스
- **종속성**: BE-403
- **추정**: 1d

---

### FE-452 — 검색 필터/정렬 UI
- **도메인**: fe / discovery
- **설명**: 카테고리 다중선택 + 가격 슬라이더 + 지역 드롭다운 + 기간 선택기 + 정렬 5종.
- **Acceptance Criteria**
  - URL 쿼리 파라미터 동기화 (새로고침 유지)
  - 필터 변경 debounce 300ms
  - 결과 0건 empty state
- **대응 TC**
  - URL sync 테스트 / 필터 조합 렌더링
- **종속성**: BE-410
- **추정**: 1d

---

### FE-453 — 위시리스트 UI
- **도메인**: fe / wishlist
- **설명**: 상품 카드 하트 버튼 + 마이페이지 위시리스트 탭.
- **Acceptance Criteria**
  - 낙관적 업데이트 + 실패 시 롤백
  - 중복 추가 409 처리
- **대응 TC**
  - 낙관적 업데이트 실패 시 롤백 테스트
- **종속성**: BE-420
- **추정**: 0.75d

---

### FE-454 — 알림 설정 UI
- **도메인**: fe / settings
- **설명**: 마이페이지 > 알림 설정 페이지 — 채널별 토글 4개 + 저장.
- **Acceptance Criteria**
  - 불러오기 + 수정 후 저장 + 성공 토스트
  - marketing 채널은 동의 문구 추가
- **종속성**: BE-430
- **추정**: 0.5d

---

### DEVOPS-460 — k6 검색 부하 테스트 시나리오
- **도메인**: devops
- **설명**: 검색 엔드포인트 QPS 100 스파이크 + p95 회귀 시트 추가.
- **Acceptance Criteria**
  - k6 스크립트 `k6/search-load.js` 작성
  - p95 < 500ms 임계값 초과 시 CI fail
- **대응 TC**: CI job에서 스테이지 환경 대상으로 수행
- **종속성**: BE-410
- **추정**: 0.5d

---

### DEVOPS-461 — 관측 지표 & 알람
- **도메인**: devops
- **설명**: 환불 실패 / 비정규화 리스너 실패 / 위시리스트 알림 debounce 스킵 과다 에 대한 메트릭 + 알람.
- **Acceptance Criteria**
  - `rental_refund_failed_total`, `rental_denorm_update_failed_total` counter 노출
  - CloudWatch 알람 3개 (PR 설명에 근거 링크)
- **종속성**: BE-405, BE-411, BE-421
- **추정**: 0.5d

---

## 일정 개요 (제안)

| Day | 병렬 작업 |
|-----|----------|
| D1 | DEVOPS-415 / BE-401 / FE-453 mock 준비 |
| D2 | BE-402 / BE-404 / BE-410 / FE-450 |
| D3 | BE-403 / BE-405 / BE-411 / FE-451 |
| D4 | BE-406 / BE-420 / FE-452 / DEVOPS-460 |
| D5 | BE-421 / BE-430 / FE-453 / FE-454 |
| D6 | BE-431 / DEVOPS-461 / Sprint 4 QA |

**Critical Path**: DEVOPS-415 → BE-401 → BE-402 → BE-403 → BE-405 → BE-406 (분쟁→환불→정산 체인 5d)

---

## 티켓 생성 형식 (Jira/Notion 복붙용)

각 티켓은 아래 필드로 동일하게 등록:

```
Title       : {ID} — {한 줄 설명}
Description : # 설명\n## Acceptance Criteria\n## 대응 TC\n## 종속성\n## 추정
Labels      : sprint-4, {domain}, {priority}
Estimate    : {0.5d / 0.75d / 1d}
Linked      : PRD-004, ADR-009, ADR-010, TDD-004
```

---

## References
- PRD-004, ADR-009, ADR-010, TDD-004
- CLAUDE.md — TDD-first, Port-Adapter, QueryDSL, Transactional 위치, BFF Facade, 이벤트 리스너 예외(Transactional 위치)
