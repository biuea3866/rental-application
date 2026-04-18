# Sprint 2 QA 리포트

> 작성일: 2026-04-18
> QA 수행일: 2026-04-18
> 환경: 로컬 (BE: 미기동, FE: localhost:3000 기동 중)
> K8s: rental-commerce-dev (AWS EKS) — AWS 자격증명 만료로 접근 불가
> 수행자: Claude Agent (sonnet)
> 범위: Sprint 2 전체 (17개 티켓, PR #49~72)

---

## 1. 전체 요약

| 항목 | 수 |
|------|---|
| 전체 테스트 케이스 | 52개 |
| PASS | 0개 |
| FAIL | 9개 (코드 분석 기반 사전 발견) |
| 미수행 (환경 접근 불가) | 43개 |

> 주의: K8s 접근 불가 및 Playwright 미설치로 인해 실제 E2E curl/브라우저 실행 검증은 수행하지 못함.
> BE API 검증 및 FE 브라우저 플로우 검증은 코드 정적 분석 + 빌드 산출물 분석으로 대체함.

---

## 2. 환경 점검 결과

| 환경 | 상태 | 세부 내용 |
|------|------|-----------|
| K8s rental-commerce-dev | FAIL | AWS 자격증명 만료 (doodlin2aws exit code 1) — kubectl 접근 불가 |
| BE API (localhost:8080) | FAIL | 로컬 미기동 (curl 타임아웃) |
| FE (localhost:3000) | 부분PASS | Next.js 기동 중이나 Sprint 2 일부 라우트 404 |
| Playwright MCP | FAIL | Extension 미설치 (Extension connection timeout) |

---

## 3. FE 화면 검증 결과 (가장 중요)

> FE는 localhost:3000에서 기동 중이나 이전 빌드(2026-04-16 23:13)로 동작 중.
> Sprint 2 RC-FE-215 (PR #59) 변경 사항이 포함되지 않은 빌드로 확인됨.

### FE 라우트별 HTTP 상태

| 라우트 | HTTP 상태 | 결과 | 비고 |
|--------|-----------|------|------|
| `/` (홈) | 200 OK | PASS | "등록자/대여자로 시작하기" 버튼 렌더링 확인 |
| `/login` | 200 OK | PASS | 이메일/비밀번호 로그인 폼 렌더링 확인 |
| `/signup` | 200 OK | PASS | 회원가입 폼 렌더링 확인 |
| `/signup/verify` | 200 OK | PASS | 전화 인증 페이지 렌더링 확인 |
| `/mypage` | 200 OK | PASS | 마이페이지 렌더링 확인 |
| `/mypage/edit` | 200 OK | PASS | 프로필 수정 페이지 렌더링 확인 |
| `/notifications` | 200 OK | PASS | 알림 목록 페이지 렌더링 확인 |
| `/lender/products` | 200 OK | PASS | 등록자 상품 목록 렌더링 확인 |
| `/login/social/callback` | 200 OK | PASS | 소셜 로그인 콜백 렌더링 확인 |
| **`/my-rentals`** | **404 Not Found** | **FAIL** | **Sprint 2 핵심 라우트 — 빌드에 미포함** |
| **`/rentals/new`** | **404 Not Found** | **FAIL** | **대여 신청 페이지 — page.js 빌드는 있으나 서버 라우팅 실패** |
| **`/rentals/{id}`** | **404 Not Found** | **FAIL** | **대여 상세 페이지 — page.js 빌드 없음** |
| **`/rentals/{id}/payment`** | **404 Not Found** | **FAIL** | **결제 페이지 — page.js 빌드는 있으나 서버 라우팅 실패** |
| `/products` | 404 Not Found | FAIL | Sprint 1 이슈 — (renter) 라우트 그룹 문제 지속 |

### FE 빌드 분석 결과

빌드 타임스탬프: 2026-04-16 23:13 (현재 실행 중인 빌드)

```
.next/server/app/
├── index.html           ✅ 200
├── login.html           ✅ 200
├── signup.html          ✅ 200
├── signup/verify.html   ✅ 200
├── mypage.html          ✅ 200
├── mypage/edit.html     ✅ 200
├── notifications.html   ✅ 200
├── lender/products.html ✅ 200
├── rentals/
│   ├── new.html         🔴 빌드 있음, 404 (라우팅 오류)
│   └── [id]/
│       └── payment/     🔴 page.js만 있음, HTML 미생성, 404
└── (없음) my-rentals    🔴 빌드 자체 없음 (RC-FE-215 미포함)
```

### FE 화면 검증 판정

| 기능 | 판정 | 이유 |
|------|------|------|
| 대여 신청 페이지 `/rentals/new` | FAIL | 404 — 빌드 있으나 라우팅 실패 |
| 결제 페이지 `/rentals/{id}/payment` | FAIL | 404 — HTML 미생성 |
| 내 대여 목록 `/my-rentals` | FAIL | 404 — 빌드 자체 없음 |
| 대여 상세 `/rentals/{id}` | FAIL | 404 — 빌드 없음 |
| 대여 신청 → 결제 → 목록 플로우 | FAIL | 진입점 404로 플로우 전체 불가 |

---

## 4. BE API 코드 정적 분석 결과

> K8s 환경 접근 불가 및 로컬 BE 미기동으로 curl 실행 불가.
> 코드 분석 및 Sprint 1 QA 결과 참조하여 검증.

### BE 구현 레이어 분석

| 레이어 | 구현 상태 | 하네스 규칙 준수 |
|--------|-----------|-----------------|
| Rental Entity (RC-BE-201) | 완료 | PASS — ZonedDateTime 사용, 상태전이 canTransitTo 캡슐화, Rich Domain Model |
| RentalDomainService (RC-BE-202) | 완료 | PASS — Repository/Gateway 경유, 이벤트 발행 캡슐화 |
| UseCase 3종 (RC-BE-203) | 완료 | PASS — DomainService만 호출, @Transactional UseCase 선언 |
| PaymentGateway Port+Adapter (RC-BE-204) | 완료 | PASS — Port-Adapter 패턴 적용 |
| ProcessPaymentUseCase (RC-BE-205) | 완료 | PASS — 멱등성 보장 (findByRentalId 조기 반환) |
| Start/Return/Cancel UseCase (RC-BE-206) | 완료 | PASS |
| QueryDSL 조회 (RC-BE-207) | 완료 | PASS — @Query 미사용, QueryDSL CustomRepository+Impl 패턴 |
| RentalApiController 9개 엔드포인트 (RC-BE-208) | 완료 | PASS |
| 단위 테스트 135 케이스 (RC-BE-209) | 완료 | PASS |
| Testcontainers 통합 테스트 (RC-BE-210) | 완료 | PASS |
| Kafka 이벤트 + Notification (RC-BE-216) | 완료 | PASS — DTO 직접 매핑, ConsumerRecord 미사용 |
| Flyway V8/V9 (RC-DEVOPS-211) | 완료 | PASS — FK 없음, ENUM 없음, DATETIME(6) 사용 |
| Kafka TopicConfig (RC-DEVOPS-212) | 완료 | PASS — RentalTopics 상수 사용 |

---

## 5. 사전 발견 버그 목록 (코드 분석 기반)

### CRITICAL

| ID | 제목 | 내용 | 영향 범위 |
|----|------|------|-----------|
| BUG-S2-001 | 취소 API HTTP 메서드 불일치 | BE: `DELETE /api/v1/rentals/{id}` vs FE: `PATCH /api/v1/rentals/{id}/cancel` | 취소 기능 전체 동작 불가 (405 Method Not Allowed) |
| BUG-S2-002 | 내 대여 목록 URL 불일치 | BE: `GET /api/v1/my-rentals` vs FE: `GET /api/v1/rentals?role=RENTER/LENDER` | FE 내 대여 목록 조회 전체 불가 (404) |
| BUG-S2-003 | RentalSummaryResult 필드 누락 | BE 응답에 `productName`, `productThumbnailUrl` 없음 — FE RentalCard 컴포넌트 렌더링 실패 | FE 대여 목록 카드 표시 불가 |
| BUG-S2-004 | RentalDetailResult 중첩 객체 구조 불일치 | BE: `renterId: Long`, `lenderId: Long` (flat) vs FE: `renter: {userId, name}`, `lender: {userId, name}` (nested) | FE 대여 상세 페이지 데이터 렌더링 전체 실패 |
| BUG-S2-005 | FE Sprint 2 핵심 라우트 404 | `/my-rentals`, `/rentals/new`, `/rentals/{id}`, `/rentals/{id}/payment` 모두 404 반환 | Sprint 2 FE 핵심 기능 전체 접근 불가 |

### MAJOR

| ID | 제목 | 내용 | 영향 범위 |
|----|------|------|-----------|
| BUG-S2-006 | GetMyRentalsUseCase role 파라미터 미지원 | `GetMyRentalsCommand`에 role 파라미터 없어 대여자/등록자 뷰 분리 불가 | 내 대여 목록 탭 전환 무의미 |
| BUG-S2-007 | Notification 신규 신청 알림 미발행 | `RentalStatusChangedEventWorker`가 `REQUESTED` 이벤트에 대해 알림 미생성 — 등록자가 신규 신청 알림 못 받음 | 등록자 신규 신청 알림 기능 없음 |

### MINOR

| ID | 제목 | 내용 | 영향 범위 |
|----|------|------|-----------|
| BUG-S2-008 | GetRentalDetailUseCase timeline 미지원 | `RentalDetailResult`에 timeline 필드 없음 (FE: `timeline: RentalTimelineEntry[]` 기대) | 대여 상세 타임라인 표시 불가 |
| BUG-S2-009 | RentalDetailResult product 정보 미포함 | BE 응답에 `product.name`, `product.thumbnailUrl`, `product.category` 없음 (FE RentalDetailPage 상품 정보 섹션) | 대여 상세 상품 정보 섹션 렌더링 실패 |

---

## 6. BE API 검증 결과 (curl)

> 환경 제약으로 실제 curl 실행 불가. 코드 분석 및 빌드 로그 기반 추론 결과 기록.

| TC | 테스트 케이스 | 결과 | 근거 |
|----|--------------|------|------|
| TC-S2-001 | POST /api/v1/rentals 정상 신청 | 미수행 | BE 미기동 |
| TC-S2-002 | 자기 상품 대여 불가 | 미수행 | BE 미기동 |
| TC-S2-003 | 기간 중복 차단 | 미수행 | BE 미기동 |
| TC-S2-004 | 미인증 접근 401 | 미수행 | BE 미기동 |
| TC-S2-007 | PATCH /approve 정상 승인 | 미수행 | BE 미기동 |
| TC-S2-013 | POST /payment 정상 결제 | 미수행 | BE 미기동 |
| TC-S2-014 | 결제 멱등성 | 미수행 | BE 미기동 |
| TC-S2-018 | PATCH /start 대여 시작 | 미수행 | BE 미기동 |
| TC-S2-021 | PATCH /return 반납 | 미수행 | BE 미기동 |
| TC-S2-023 | DELETE /rentals/{id} 취소 | 미수행 (BUG-S2-001 사전 발견) | FE는 PATCH /cancel로 호출 |
| TC-S2-027 | GET /my-rentals 목록 | 미수행 (BUG-S2-002 사전 발견) | FE는 GET /rentals로 호출 |
| TC-S2-031 | GET /rentals/{id} 상세 | 미수행 | BE 미기동 |
| TC-S2-035 | Kafka 이벤트 발행 | 미수행 | K8s 접근 불가 |
| TC-S2-037 | 승인 시 알림 생성 | 미수행 | K8s 접근 불가 |

---

## 7. FE 플로우 검증 결과

> Playwright MCP Extension 미설치로 브라우저 자동화 불가.
> curl HEAD 요청 및 Next.js 빌드 아티팩트 분석으로 대체.

### 핵심 유저 플로우 검증

| 플로우 | 단계 | 결과 | 세부 내용 |
|--------|------|------|-----------|
| 대여 신청 플로우 | `/products/{id}` → `/rentals/new?productId={id}` | FAIL | `/rentals/new` 404 반환 |
| 결제 플로우 | `/rentals/{id}` → `/rentals/{id}/payment` | FAIL | `/rentals/{id}` 404 반환 |
| 내 대여 목록 | `/my-rentals` | FAIL | `/my-rentals` 404 반환 |
| 대여 상세 | `/rentals/{id}` | FAIL | `/rentals/{id}` 404 반환 |
| 승인/거절 액션 | FE 상세 페이지 RentalActionButtons | FAIL | 상세 페이지 접근 불가 |
| 취소 액션 | FE cancelRentalApi (PATCH /cancel) | FAIL | BE는 DELETE 사용 — 메서드 불일치 |

### FE 컴포넌트 코드 분석 (Sprint 2 구현 확인)

| 컴포넌트 | 구현 상태 | 주요 기능 |
|----------|-----------|-----------|
| `RentalRequestForm` | 구현 완료 | 기간 선택 + 배송지 입력 + 신청 버튼 |
| `RentalCard` | 구현 완료 | 대여 요약 카드 (productName 필요 — BUG-S2-003) |
| `RentalStatusTimeline` | 구현 완료 | 상태 진행 타임라인 (timeline 배열 필요 — BUG-S2-008) |
| `RentalActionButtons` | 구현 완료 | 상태별 승인/거절/결제/시작/반납/취소 버튼 |
| `PaymentWidget` | 구현 완료 | Toss Payments 위젯 연동 |
| `MyRentalsPage` | 구현 완료 | 탭 + 상태 필터 + 목록 (URL/role 불일치 — BUG-S2-002/006) |
| `RentalDetailPage` | 구현 완료 | 상태 타임라인 + 상품/대여/요금/결제 정보 (스키마 불일치 — BUG-S2-004/009) |
| `RentalPaymentPage` | 구현 완료 | 결제 위젯 + 최종 금액 표시 |

---

## 8. Kafka / Notification 검증 결과

> K8s 접근 불가로 실 환경 검증 불가. 코드 분석 결과:

| 항목 | 분석 결과 |
|------|-----------|
| 토픽 명명 | `event.rental.status-changed` — 하네스 규칙 `event.{service}.{domain}` 준수 |
| Consumer DTO 매핑 | `RentalStatusChangedKafkaConsumer` → `RentalStatusChangedEvent` DTO 직접 수신 (ConsumerRecord 미사용) |
| Notification Worker | APPROVED/RETURNED/CANCELLED 3가지 상태에 알림 생성 (REQUESTED 미생성 — BUG-S2-007) |
| 알림 대상 | APPROVED → renterId, RETURNED → lenderId, CANCELLED → renterId |
| Facade 경유 | `RentalStatusChangedEventWorker` → `NotificationDomainService` 경유 (Repository 직접 호출 없음) |

---

## 9. 하네스 규칙 위반 점검

| 규칙 | 결과 | 세부 내용 |
|------|------|-----------|
| no-jpa-query (QueryDSL 사용 필수) | PASS | RentalQueryRepositoryImpl — @Query 없음, QueryDSL BooleanBuilder 사용 |
| no-consumer-record (DTO 직접 매핑) | PASS | RentalStatusChangedKafkaConsumer — event: RentalStatusChangedEvent 직접 수신 |
| no-local-datetime (ZonedDateTime 사용) | PASS | Rental 엔티티 — ZonedDateTime 사용 확인 |
| no-repo-in-usecase (UseCase → DomainService만) | PASS | 모든 UseCase — rentalDomainService/productDomainService만 참조 |
| no-kafka-topic-hardcode (RentalTopics 상수) | PASS | RentalTopics.STATUS_CHANGED 상수 사용 |
| no-cross-domain-import | PASS | rental UseCase — product를 직접 import하지 않고 DomainService 경유 |
| @Transactional UseCase 선언 | PASS | 모든 UseCase에 @Transactional 선언 확인 |
| no-double-bang (!!) | 미검증 — 코드 스캔 필요 | 전체 파일 스캔 미수행 |

---

## 10. 테스트 커버리지 요약

| 레이어 | 테스트 타입 | 테스트 케이스 수 | 상태 |
|--------|------------|----------------|------|
| Domain (Rental Entity, Status) | 단위 테스트 (Kotest) | 135 케이스 | 구현 완료 (RC-BE-209) |
| Application (UseCase) | 단위 테스트 (MockK) | 포함 (135 내) | 구현 완료 |
| Infrastructure (Kafka, QueryDSL, Repository) | Testcontainers 통합 테스트 | 5 케이스 (RC-BE-210) | 구현 완료 |
| FE (Unit + Page Tests) | Vitest + Testing Library | my-rentals, rental-detail 등 | 구현 완료 (RC-FE-215) |
| E2E (Playwright) | 미수행 | - | Extension 미설치 |
| 부하 테스트 (k6) | 동시성 시나리오 | RC-DEVOPS-213 | 구현 완료 (실행 미확인) |

---

## 11. 최종 판정

**판정: NO-GO**

### 판정 근거

1. **FE Sprint 2 핵심 라우트 전부 404** (BUG-S2-005 CRITICAL)
   - `/my-rentals`, `/rentals/new`, `/rentals/{id}`, `/rentals/{id}/payment` 모두 접근 불가
   - Sprint 2 FE 코드(RC-FE-215, RC-FE-214)는 구현되었으나 현재 빌드에 미포함 — 재빌드 필요

2. **BE-FE 스키마 불일치 4건** (BUG-S2-001~004 CRITICAL)
   - 취소 API HTTP 메서드 불일치 (DELETE vs PATCH)
   - 내 대여 목록 URL + role 파라미터 불일치
   - RentalSummaryResult 필드 누락 (productName, productThumbnailUrl)
   - RentalDetailResult 중첩 객체 구조 불일치 (flat vs nested)

3. **환경 접근 불가로 실 API 검증 미완료**
   - K8s AWS 자격증명 만료 — BE 실제 동작 검증 불가
   - Playwright Extension 미설치 — 브라우저 E2E 검증 불가

### NO-GO 해소 조건

| 조건 | 담당 |
|------|------|
| BUG-S2-001: 취소 API HTTP 메서드 통일 (BE DELETE → PATCH /cancel 추가 또는 FE 수정) | BE/FE |
| BUG-S2-002: 내 대여 목록 URL 통일 (`/my-rentals` → `/rentals` + role 파라미터 지원) | BE/FE |
| BUG-S2-003: RentalSummaryResult에 productName/productThumbnailUrl 추가 | BE |
| BUG-S2-004: RentalDetailResult에 renter/lender 중첩 객체 추가 | BE |
| BUG-S2-005: FE 재빌드 + Sprint 2 라우트 접근 가능 확인 | FE/DevOps |
| K8s 자격증명 갱신 후 BE curl 검증 52개 TC 전부 수행 | DevOps/QA |
| Playwright 브라우저 E2E 플로우 검증 (신청→결제→목록→상세) | QA |

---

## 12. 부록: 검증 실행 명령어

### BE curl 검증 (환경 복구 후 실행)

```bash
# port-forward
kubectl -n rental-commerce-dev port-forward svc/rental-api 8080:8080

# TC-S2-001: 정상 대여 신청
curl -X POST http://localhost:8080/api/v1/rentals \
  -H "Content-Type: application/json" \
  -H "X-Member-Id: 3" \
  -d '{"productId":1,"startDate":"2026-05-01T00:00:00+09:00","endDate":"2026-05-07T00:00:00+09:00","deliveryInfo":{"recipientName":"홍길동","recipientPhone":"010-1234-5678","addressLine1":"서울 강남구 테헤란로 123","zipCode":"06234"}}'

# TC-S2-002: 자기 상품 대여 불가
curl -X POST http://localhost:8080/api/v1/rentals \
  -H "Content-Type: application/json" \
  -H "X-Member-Id: 1" \
  -d '{"productId":1,"startDate":"2026-05-01T00:00:00+09:00","endDate":"2026-05-07T00:00:00+09:00","deliveryInfo":{"recipientName":"test","recipientPhone":"010-0000-0000","addressLine1":"test","zipCode":"00000"}}'

# TC-S2-004: 미인증 접근 401
curl -X POST http://localhost:8080/api/v1/rentals \
  -H "Content-Type: application/json" \
  -d '{"productId":1}'

# TC-S2-007: 승인
curl -X PATCH http://localhost:8080/api/v1/rentals/1/approve \
  -H "X-Member-Id: 2"

# TC-S2-013: 결제
curl -X POST http://localhost:8080/api/v1/rentals/1/payment \
  -H "Content-Type: application/json" \
  -H "X-Member-Id: 3" \
  -d '{"paymentKey":"mock_key_001","orderId":"RC-1-1713456789000","amount":120000,"paymentMethod":"CARD"}'

# TC-S2-023: 취소 (BE는 DELETE)
curl -X DELETE http://localhost:8080/api/v1/rentals/1 \
  -H "Content-Type: application/json" \
  -H "X-Member-Id: 3" \
  -d '{"reason":"테스트 취소"}'

# TC-S2-027: 내 대여 목록
curl -X GET "http://localhost:8080/api/v1/my-rentals" \
  -H "X-Member-Id: 3"
```

### FE 재빌드 (Sprint 2 라우트 활성화)

```bash
cd /Users/biuea/feature/flag_project/rental-commerce/rental-web
npm run build
# 빌드 완료 후 my-rentals, rentals/[id] 라우트 포함 확인
```
