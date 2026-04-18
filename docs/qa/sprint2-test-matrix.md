# Sprint 2 테스트 매트릭스

> 작성일: 2026-04-14
> 환경: rental-commerce-dev (Kubernetes)
> BE: http://localhost:8080 (kubectl -n rental-commerce-dev port-forward svc/rental-api 8080:8080)
> FE: http://localhost:3000 (kubectl -n rental-commerce-dev port-forward svc/rental-web 3000:3000)
> Sprint 2 범위: RC-BE-201~216, RC-FE-214~215, RC-DEVOPS-211~213

---

## Sprint 2 구현 범위 (17개 티켓 전부 머지)

| 티켓 | 내용 | PR |
|------|------|----|
| RC-BE-201 | Rental 도메인 엔티티 + 상태 전이 + TDD | #50 |
| RC-BE-202 | RentalDomainService — 기간 중복 검증 + 금액 계산 | #51 |
| RC-BE-203 | RequestRental + ApproveRental + RejectRental UseCase | #52 |
| RC-BE-204 | PaymentGateway Port + TossPayment/Mock Adapter | #53 |
| RC-BE-205 | ProcessPaymentUseCase — 결제 처리 + 멱등성 보장 | #54 |
| RC-BE-206 | StartRental + ReturnRental + CancelRental UseCase | #55 |
| RC-BE-207 | QueryDSL 대여 조회 — GetMyRentals + GetRentalDetail | #68 |
| RC-BE-208 | RentalApiController — 9개 엔드포인트 | #56 |
| RC-BE-209 | 대여/결제 단위 테스트 보강 — 135 케이스 | #69 |
| RC-BE-210 | 대여/결제 통합 테스트 — Testcontainers 5 케이스 | #71 |
| RC-BE-216 | Kafka 이벤트 발행 + Notification 연동 | #70 |
| RC-FE-214 | 대여 신청 + 결제 페이지 | #58 |
| RC-FE-215 | 내 대여 목록 + 대여 상세 + 등록자 관리 | #59 |
| RC-DEVOPS-211 | Flyway V8/V9 — rental + rental_payment 테이블 | #49 |
| RC-DEVOPS-212 | Kafka RentalTopics + TopicConfig | #65 |
| RC-DEVOPS-213 | k6 동시성 부하 테스트 시나리오 | #72 |

---

## 테스트 매트릭스

| 번호 | 도메인 | 테스트 케이스 | 전제 조건 | 기대 결과 | BE 검증 | FE 검증 | 결과 | 비고 |
|------|--------|--------------|-----------|-----------|---------|---------|------|------|
| **[대여 신청]** | | | | | | | | |
| TC-S2-001 | 대여 신청 | 정상 대여 신청 (POST /api/v1/rentals) | 인증된 대여자, APPROVED 상품, 기간 미중복 | 201 Created, rentalId + REQUESTED 상태 반환 | 미수행 | 미수행 | 미수행 | |
| TC-S2-002 | 대여 신청 | 자기 상품 대여 불가 — self-rental 방지 | 자기가 등록한 상품 productId 전달 | 400 or 403, SELF_RENTAL_FORBIDDEN 에러 | 미수행 | 미수행 | 미수행 | Entity.validateNotOwnedBy 캡슐화 검증 |
| TC-S2-003 | 대여 신청 | 기간 중복 신청 차단 | 동일 상품 동일 기간 이미 REQUESTED/APPROVED/PAID/IN_USE 건 존재 | 409, RENTAL_PERIOD_CONFLICT 에러 | 미수행 | 미수행 | 미수행 | RentalDomainService.validatePeriodAvailability |
| TC-S2-004 | 대여 신청 | 미인증 접근 차단 | Authorization 헤더 없음 | 401 Unauthorized | 미수행 | 미수행 | 미수행 | JwtAuthenticationFilter |
| TC-S2-005 | 대여 신청 | APPROVED 상태 아닌 상품 대여 불가 | status=DRAFT 상품 productId 전달 | 400, PRODUCT_NOT_AVAILABLE 에러 | 미수행 | 미수행 | 미수행 | Product.validateAvailableForRental |
| TC-S2-006 | 대여 신청 | 존재하지 않는 상품 신청 | 없는 productId=99999 | 404, PRODUCT_NOT_FOUND 에러 | 미수행 | 미수행 | 미수행 | |
| **[승인/거절]** | | | | | | | | |
| TC-S2-007 | 승인 | 등록자 정상 승인 (PATCH /api/v1/rentals/{id}/approve) | REQUESTED 상태 대여, 등록자 권한 | 200 OK, 상태 REQUESTED → APPROVED | 미수행 | 미수행 | 미수행 | |
| TC-S2-008 | 승인 | 대여자가 승인 시도 → 권한 거절 | 대여자 userId로 승인 요청 | 403, FORBIDDEN 에러 | 미수행 | 미수행 | 미수행 | rental.verifyLenderAuthority 캡슐화 |
| TC-S2-009 | 승인 | APPROVED 상태에서 재승인 시도 | 이미 APPROVED인 대여 승인 | 409, INVALID_STATE_TRANSITION 에러 | 미수행 | 미수행 | 미수행 | RentalStatus.canTransitTo 검증 |
| TC-S2-010 | 거절 | 등록자 정상 거절 (PATCH /api/v1/rentals/{id}/reject) | REQUESTED 상태 대여, 거절 사유 포함 | 200 OK, 상태 REQUESTED → CANCELLED | 미수행 | 미수행 | 미수행 | |
| TC-S2-011 | 거절 | 거절 사유 빈 문자열 전송 | reason="" | 400, 유효성 검증 에러 | 미수행 | 미수행 | 미수행 | |
| TC-S2-012 | 거절 | 이미 PAID된 건 거절 시도 | PAID 상태 대여 reject | 409, INVALID_STATE_TRANSITION 에러 | 미수행 | 미수행 | 미수행 | |
| **[결제]** | | | | | | | | |
| TC-S2-013 | 결제 | 정상 결제 처리 (POST /api/v1/rentals/{id}/payment) | APPROVED 상태 대여, 대여자 권한 | 200 OK, 상태 APPROVED → PAID, paymentId 반환 | 미수행 | 미수행 | 미수행 | MockPaymentGatewayAdapter 사용 |
| TC-S2-014 | 결제 | 결제 멱등성 — 동일 orderId 재요청 | 이미 PAID된 대여에 동일 orderId 재전송 | 200 OK, 기존 RentalPayment 반환 (중복 결제 없음) | 미수행 | 미수행 | 미수행 | findByRentalId 조기 반환 로직 검증 |
| TC-S2-015 | 결제 | APPROVED 외 상태 결제 시도 | REQUESTED 상태 대여 결제 | 409, INVALID_STATE_TRANSITION 에러 | 미수행 | 미수행 | 미수행 | validatePaymentTransition 검증 |
| TC-S2-016 | 결제 | 대여자 아닌 사용자 결제 시도 | 등록자 userId로 결제 요청 | 403, FORBIDDEN 에러 | 미수행 | 미수행 | 미수행 | rental.verifyRenterAuthority 캡슐화 |
| TC-S2-017 | 결제 | 금액 불일치 결제 시도 | amount != rental.totalAmount 전송 | 400/409, 결제 실패 에러 | 미수행 | 미수행 | 미수행 | PaymentGateway 검증 |
| **[시작/반납]** | | | | | | | | |
| TC-S2-018 | 대여 시작 | 등록자 정상 시작 (PATCH /api/v1/rentals/{id}/start) | PAID 상태, 등록자 권한 | 200 OK, 상태 PAID → IN_USE | 미수행 | 미수행 | 미수행 | |
| TC-S2-019 | 대여 시작 | 대여자가 시작 시도 → 권한 거절 | 대여자 userId로 start 요청 | 403, FORBIDDEN 에러 | 미수행 | 미수행 | 미수행 | verifyLenderAuthority |
| TC-S2-020 | 대여 시작 | REQUESTED 상태 시작 시도 | REQUESTED 상태 start 요청 | 409, INVALID_STATE_TRANSITION 에러 | 미수행 | 미수행 | 미수행 | |
| TC-S2-021 | 반납 | 정상 반납 처리 (PATCH /api/v1/rentals/{id}/return) | IN_USE 상태 대여 | 200 OK, 상태 IN_USE → RETURNED | 미수행 | 미수행 | 미수행 | |
| TC-S2-022 | 반납 | IN_USE 외 상태 반납 시도 | PAID 상태 return 요청 | 409, INVALID_STATE_TRANSITION 에러 | 미수행 | 미수행 | 미수행 | |
| **[취소]** | | | | | | | | |
| TC-S2-023 | 취소 | REQUESTED 상태 취소 (DELETE /api/v1/rentals/{id}) | REQUESTED 상태, 취소 사유 포함 | 200 OK, 상태 REQUESTED → CANCELLED, 환불 없음 | 미수행 | 미수행 | 미수행 | **주의: BE는 DELETE, FE는 PATCH /cancel — HTTP 메서드 불일치** |
| TC-S2-024 | 취소 | APPROVED 상태 취소 | APPROVED 상태 취소 | 200 OK, 상태 APPROVED → CANCELLED, 환불 없음 | 미수행 | 미수행 | 미수행 | |
| TC-S2-025 | 취소 | PAID 상태 취소 → 환불 | PAID 상태 취소 | 200 OK, 상태 PAID → CANCELLED, PaymentGateway.cancelPayment 호출 + payment.refund() | 미수행 | 미수행 | 미수행 | refundIfCompleted 로직 검증 |
| TC-S2-026 | 취소 | IN_USE 상태 취소 시도 | IN_USE 상태 취소 | 409, INVALID_STATE_TRANSITION 에러 | 미수행 | 미수행 | 미수행 | RETURNED만 전이 가능 |
| **[조회 — QueryDSL]** | | | | | | | | |
| TC-S2-027 | 조회 | 내 대여 목록 — 대여자 뷰 (GET /api/v1/my-rentals) | 인증 유저, role=RENTER 파라미터 | 200 OK, PageResult<RentalSummaryResult> 반환 | 미수행 | 미수행 | 미수행 | **주의: BE /my-rentals, FE GET /rentals?role=RENTER — URL 불일치** |
| TC-S2-028 | 조회 | 내 대여 목록 — 등록자 뷰 (GET /api/v1/my-rentals) | role=LENDER 파라미터 | 200 OK, lenderId 기준 목록 반환 | 미수행 | 미수행 | 미수행 | GetMyRentalsUseCase에 role 파라미터 없음 — userId로만 조회 |
| TC-S2-029 | 조회 | 내 대여 목록 — 상태 필터 | status=REQUESTED | 200 OK, REQUESTED 상태만 필터링 | 미수행 | 미수행 | 미수행 | RentalQueryCondition.statusFilter |
| TC-S2-030 | 조회 | 내 대여 목록 — 페이지네이션 | page=0&size=5 | 200 OK, totalElements/totalPages 포함 PageResult | 미수행 | 미수행 | 미수행 | |
| TC-S2-031 | 조회 | 대여 상세 조회 (GET /api/v1/rentals/{id}) | 참여자(대여자 or 등록자) 접근 | 200 OK, RentalDetailResult + 결제 정보 포함 | 미수행 | 미수행 | 미수행 | QueryDSL LEFT JOIN rental+rental_payment |
| TC-S2-032 | 조회 | 대여 상세 — 비참여자 접근 차단 | 무관한 userId로 상세 조회 | 403, FORBIDDEN 에러 | 미수행 | 미수행 | 미수행 | rental.verifyParticipant 캡슐화 |
| TC-S2-033 | 조회 | BE 응답 스키마 — RentalSummaryResult | GET /api/v1/my-rentals 응답 확인 | rentalId, productId, status, startDate, endDate, totalAmount, depositAmount, requestedAt | 미수행 | 미수행 | 미수행 | **FE 기대 필드(productName, productThumbnailUrl) 없음 — CRITICAL 스키마 불일치** |
| TC-S2-034 | 조회 | BE 응답 스키마 — RentalDetailResult | GET /api/v1/rentals/{id} 응답 확인 | renterId/lenderId as Long ID | 미수행 | 미수행 | 미수행 | **FE 기대: renter.{userId,name}, lender.{userId,name} 중첩 객체 — CRITICAL 스키마 불일치** |
| **[Kafka 이벤트]** | | | | | | | | |
| TC-S2-035 | Kafka | 대여 신청 이벤트 발행 확인 | POST /rentals 성공 후 | Kafka 토픽 event.rental.status-changed에 메시지 발행 | 미수행 | N/A | 미수행 | KafkaRentalEventPublisher |
| TC-S2-036 | Kafka | 상태 변경 시 이벤트 발행 (승인/거절/결제/시작/반납/취소) | 각 상태 전이 API 호출 | 해당 이벤트 Kafka 토픽에 발행됨 | 미수행 | N/A | 미수행 | |
| TC-S2-037 | Notification | 승인 시 대여자에게 알림 생성 | APPROVED 이벤트 소비 후 | notification 테이블에 RENTAL_CONFIRMED 타입 알림 INSERT | 미수행 | 미수행 | 미수행 | RentalStatusChangedEventWorker.APPROVED 분기 |
| TC-S2-038 | Notification | 반납 시 등록자에게 알림 생성 | RETURNED 이벤트 소비 후 | RENTAL_COMPLETED 타입 알림, lenderId로 생성 | 미수행 | 미수행 | 미수행 | |
| TC-S2-039 | Notification | 취소 시 대여자에게 알림 생성 | CANCELLED 이벤트 소비 후 | RENTAL_CANCELLED 타입 알림, renterId로 생성 | 미수행 | 미수행 | 미수행 | |
| **[FE 화면 — 가장 중요]** | | | | | | | | |
| TC-S2-040 | FE 대여 신청 | 대여 신청 페이지 렌더링 (/rentals/new?productId=1) | 로그인 + APPROVED 상품 존재 | 상품 요약 + RentalRequestForm 렌더링 | N/A | 미수행 | 미수행 | RC-FE-214 |
| TC-S2-041 | FE 대여 신청 | RentalRequestForm — 기간/배송지 입력 후 신청 | 날짜 + 배송지 입력 | POST /api/v1/rentals 201, 신청 완료 | N/A | 미수행 | 미수행 | |
| TC-S2-042 | FE 결제 | 결제 페이지 렌더링 (/rentals/{id}/payment) | APPROVED 상태 대여 | 주문정보 + PaymentWidget + 최종금액 섹션 렌더링 | N/A | 미수행 | 미수행 | RC-FE-214 |
| TC-S2-043 | FE 결제 | 결제 성공 후 /rentals/{id}/payment/success 이동 | PaymentWidget 성공 콜백 | POST /api/v1/rentals/{id}/payment 호출 → 성공 페이지 이동 | N/A | 미수행 | 미수행 | |
| TC-S2-044 | FE 대여 목록 | 내 대여 목록 페이지 (/my-rentals) | 로그인 유저 | 대여자/등록자 탭 + 상태 필터 + 목록 렌더링 | N/A | 미수행 | 미수행 | RC-FE-215 |
| TC-S2-045 | FE 대여 목록 | 대여자/등록자 탭 전환 | RENTER 탭 → LENDER 탭 클릭 | 탭 변경 → 목록 재조회 | N/A | 미수행 | 미수행 | **주의: BE /my-rentals는 role 파라미터 없음 — FE role 전달 무의미** |
| TC-S2-046 | FE 대여 목록 | 상태 필터 — REQUESTED 선택 | 상태 필터 버튼 클릭 | 필터 적용 후 REQUESTED 목록만 표시 | N/A | 미수행 | 미수행 | **주의: BE GetMyRentalsCommand role 파라미터 없어서 대여자/등록자 구분 불가** |
| TC-S2-047 | FE 대여 상세 | 대여 상세 페이지 (/rentals/{id}) | 대여 ID | 상태 타임라인 + 상품정보 + 대여정보 + 요금정보 섹션 | N/A | 미수행 | 미수행 | RC-FE-215 |
| TC-S2-048 | FE 대여 상세 | RentalStatusTimeline 렌더링 | RentalDetail 데이터 | 타임라인 각 상태 노드 표시 | N/A | 미수행 | 미수행 | |
| TC-S2-049 | FE 대여 상세 | 대여자 — 결제 버튼 클릭 (APPROVED 상태) | 현재 유저 = 대여자, 상태 = APPROVED | /rentals/{id}/payment 이동 | N/A | 미수행 | 미수행 | |
| TC-S2-050 | FE 대여 상세 | 등록자 — 승인 버튼 클릭 (REQUESTED 상태) | 현재 유저 = 등록자, 상태 = REQUESTED | PATCH /rentals/{id}/approve 호출 → 화면 갱신 | N/A | 미수행 | 미수행 | |
| TC-S2-051 | FE 대여 상세 | 취소 버튼 — PATCH /cancel vs DELETE 불일치 확인 | 취소 버튼 클릭 | **CRITICAL: FE PATCH /cancel, BE DELETE /** | N/A | 미수행 | 미수행 | HTTP 메서드 불일치 — 405 응답 예상 |
| TC-S2-052 | FE 상태 타임라인 | REQUESTED→APPROVED→PAID→IN_USE 전체 타임라인 | IN_USE 상태 대여 | 4단계 타임라인 완전 표시 | N/A | 미수행 | 미수행 | |

---

## 사전 발견 스키마 불일치 목록 (코드 분석 기반)

| 번호 | 심각도 | 내용 | BE 실제 | FE 기대 | 관련 TC |
|------|--------|------|---------|---------|---------|
| M-001 | CRITICAL | 취소 API HTTP 메서드 불일치 | DELETE /api/v1/rentals/{id} | PATCH /api/v1/rentals/{id}/cancel | TC-S2-023, TC-S2-051 |
| M-002 | CRITICAL | 내 대여 목록 URL + role 파라미터 없음 | GET /api/v1/my-rentals (role 파라미터 없음) | GET /api/v1/rentals?role=RENTER/LENDER | TC-S2-027, TC-S2-045 |
| M-003 | CRITICAL | RentalSummaryResult 필드 누락 | productId, status, startDate, endDate, totalAmount, depositAmount, requestedAt | + productName, + productThumbnailUrl | TC-S2-033, TC-S2-044 |
| M-004 | CRITICAL | RentalDetailResult 중첩 객체 구조 불일치 | renterId: Long, lenderId: Long (flat) | renter: {userId, name}, lender: {userId, name} (nested) | TC-S2-034, TC-S2-047 |
| M-005 | MAJOR | GetMyRentalsUseCase role 파라미터 없음 | userId로만 조회 (대여자+등록자 혼합) | role=RENTER → 대여자 건만, role=LENDER → 등록자 건만 | TC-S2-028, TC-S2-046 |
| M-006 | MINOR | Notification Worker — REQUESTED 이벤트 알림 없음 | APPROVED/RETURNED/CANCELLED만 알림 생성 | 신규 신청 시 등록자 알림 기대 가능 | TC-S2-039 |
