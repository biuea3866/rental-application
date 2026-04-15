# Sprint 1 테스트 매트릭스

> 작성일: 2026-04-15  
> 환경: rental-commerce-dev (Kubernetes)  
> BE: http://localhost:18080 (kubectl port-forward svc/rental-api 18080:80)  
> FE: http://localhost:3000 (kubectl port-forward svc/rental-web 3000:3000)

---

| 번호 | 도메인 | 플로우 | 기대 결과 | BE 검증 | FE 검증 | 크로스 검증 | 결과 | 비고 |
|------|--------|--------|----------|---------|---------|-------------|------|------|
| **[Auth]** | | | | | | | | |
| TC-001 | Auth | 이메일 회원가입 (POST /api/v1/auth/signup) | 200 OK, 인증코드 발송 메시지 + phone/email 반환 | PASS ({"email":"qa_lender@test.com","phone":"01011112222","message":"인증코드가 발송되었습니다"}) | 부분PASS (signup 페이지 200, 폼 렌더링 OK) | MAJOR: FE SignupRequest는 email/password/name/phone/role 필드 전송하나 BE signup은 즉시 토큰 미발급, verify-phone으로 2단계 필요 | PASS | 2단계 인증 플로우 |
| TC-002 | Auth | 전화 인증 완료 (POST /api/v1/auth/verify-phone) | 200 OK, accessToken/refreshToken/tokenFamily/userId 반환 | PASS (JWT 발급 확인) | 부분PASS (FE /signup/verify 200 로딩중...) | MAJOR: BE VerifyPhoneRequest는 email/phone/code/password/name/role 요구, FE types에는 VerifyPhoneRequest가 {phone, code}만 정의됨 (필드 불일치) | PASS(BE)/FAIL(FE-타입) | FE 타입 불일치 이슈 |
| TC-003 | Auth | 로그인 (POST /api/v1/auth/login) | 200 OK, accessToken/refreshToken/tokenFamily/userId 반환 | PASS | PASS (login 페이지 200, 이메일/비밀번호 입력폼 정상 렌더링) | MINOR: FE AuthTokens 타입에 tokenFamily 필드 없음 | PASS(BE)/MINOR(FE) | FE types.ts에 tokenFamily 미정의 |
| TC-004 | Auth | 로그아웃 (POST /api/v1/auth/logout) | - | FAIL: BE에 /api/v1/auth/logout 엔드포인트 없음 (404) | FAIL (FE useAuth logout이 POST /api/v1/auth/logout 호출 시도) | CRITICAL: FE ENDPOINTS.AUTH.LOGOUT = /api/v1/auth/logout 이나 BE에 해당 엔드포인트 미구현 | FAIL | BLOCKER 후보 - BE 미구현 |
| TC-005 | Auth | JWT 토큰 갱신 (POST /api/v1/auth/refresh) | 200 OK, 새 accessToken 발급 | PASS (refreshToken + tokenFamily 모두 보낼 때) | FAIL (FE RefreshTokenRequest에 tokenFamily 필드 없어서 400 발생) | CRITICAL: FE types.ts RefreshTokenRequest = {refreshToken} 이나 BE = {refreshToken, tokenFamily} 필수 | FAIL | FE-BE 스키마 불일치 |
| TC-006 | Auth | 카카오 OAuth (POST /api/v1/auth/social-login) | 외부 API 연동 필요 (단위테스트: 400/500) | 부분PASS: 필드명 mismatch 발견 후 authorizationCode 사용 시 외부API 500 | PASS (소셜로그인 버튼 렌더링 확인) | CRITICAL: FE SocialLoginRequest = {provider, code, redirectUri}, BE = {provider, authorizationCode} (code vs authorizationCode 불일치) | FAIL | FE-BE 필드명 불일치 |
| TC-007 | Auth | 네이버 OAuth (POST /api/v1/auth/social-login) | 외부 API 연동 필요 | 미수행 (카카오와 동일 코드 경로) | PASS (네이버 버튼 렌더링 확인) | 동일 CRITICAL 이슈 | FAIL | TC-006과 동일 |
| TC-008 | Auth | X-Member-Id 헤더 인증 | 인증 헤더 없으면 401 | PASS (401 응답 정상) | N/A (FE client.ts에서 헤더 자동 추가) | - | PASS | |
| TC-009 | Auth | GET /api/v1/auth/me (유저 정보 조회) | 200 OK | FAIL: BE에 /api/v1/auth/me 엔드포인트 없음 (404) | FAIL (FE 로그인 후 자동 호출 시 실패) | BLOCKER: FE useAuth.login()이 토큰 저장 후 GET /api/v1/auth/me 호출하나 BE 미구현. 로그인 후 유저 정보 세팅 불가 | FAIL | BLOCKER |
| **[User Profile]** | | | | | | | | |
| TC-010 | User Profile | LenderProfile 생성 (POST /api/v1/users/me/lender-profile) | 201 Created | PASS ({"lenderProfileId":1,"userId":1,"lenderType":"INDIVIDUAL","verificationStatus":"PENDING",...}) | 미수행 (Playwright 불가) | MAJOR: FE UpdateLenderProfileRequest = {businessName, description, bankName, bankAccount}, BE CreateLenderProfileRequest = {lenderType}만 요구 (필드 완전 불일치) | PASS(BE) | |
| TC-011 | User Profile | LenderProfile 조회 (GET /api/v1/users/me/lender-profile) | 200 OK | PASS (lenderProfile 데이터 반환 확인) | 미수행 | - | PASS | |
| TC-012 | User Profile | LenderProfile 수정 (PATCH /api/v1/mypage/lender-profile) | 200 OK | PASS (settlementAccountBank/Number 업데이트 확인) | 미수행 | MINOR: FE LenderProfile 타입에 verificationStatus, lenderType 필드 없음 | PASS(BE) | |
| TC-013 | User Profile | RenterProfile 생성 (POST /api/v1/users/me/renter-profile) | 201 Created | PASS ({"renterProfileId":1,"trustGrade":"BRONZE","totalTransactionCount":0,...}) | 미수행 | MINOR: FE RenterProfile에 trustGrade, totalTransactionCount 필드 없음 | PASS(BE) | |
| TC-014 | User Profile | RenterProfile 조회 (GET /api/v1/users/me/renter-profile) | 200 OK | PASS | 미수행 | - | PASS | |
| TC-015 | User Profile | RenterProfile 수정 (PATCH /api/v1/mypage/renter-profile) | 200 OK, shippingAddress 업데이트 | PASS (shippingAddress 업데이트 확인) | 미수행 | - | PASS | |
| TC-016 | User Profile | 배송지 추가/삭제 (shipping_address) | shippingAddress 문자열 업데이트 | 부분PASS (단일 문자열로 저장, 복수 주소 지원 미구현) | 미수행 | MAJOR: FE RenterProfile.deliveryAddress(string) vs BE shippingAddress(string) 필드명 다름 | PASS(BE)/FAIL(FE타입) | 필드명 불일치 |
| TC-017 | User Profile | MyPage 조회 (GET /api/v1/mypage) | 200 OK, 유저정보 + 프로필 포함 | PASS (userId, email, name, phone, role, renterProfile 확인) | PASS (mypage 페이지 200 로딩, "불러오는 중..." 표시 - SSR) | - | PASS | |
| TC-018 | User Profile | 프로필 수정 (PATCH /api/v1/mypage/profile) | 204 No Content | PASS | 부분PASS (mypage/edit 라우트 존재) | - | PASS | |
| **[Product - 등록자]** | | | | | | | | |
| TC-019 | Product | Draft 생성 (POST /api/v1/products/drafts) | 201 Created, DRAFT 상태 | PASS ({"id":1,"status":"DRAFT","currentDraftStep":1,...}) | PASS (FE /lender/products 200, "내 등록 상품" + "상품 등록" 버튼 확인) | - | PASS | |
| TC-020 | Product | Draft 수정 Step1 - 기본정보 (PATCH /api/v1/products/drafts/{id}) | 200 OK, name/categoryCode 업데이트 | PASS | 미수행 | MINOR: step 파라미터가 별도로 필요함, FE가 단순 PATCH로 처리할 경우 step 누락 가능성 | PASS(BE) | |
| TC-021 | Product | Draft 수정 Step2 - 가격 (PATCH /api/v1/products/drafts/{id}) | 200 OK, prices 배열 업데이트 | PASS (DAILY/MONTHLY/YEARLY 단위) | 미수행 | CRITICAL: FE Product.pricePerDay(number)는 단일값이나 BE prices[]는 배열. FE 상품등록 폼에서 가격 표시 불가 | PASS(BE)/FAIL(FE타입) | 타입 구조 불일치 |
| TC-022 | Product | Draft 수정 - RentalUnit enum 불일치 | DAILY/MONTHLY/YEARLY만 유효 | PASS (DAY 전송 시 400 반환 확인) | 미수행 | MAJOR: FE useProducts hook의 sort 파라미터가 "latest"/"price_asc"/"price_desc"이나 BE ProductSortBy = CREATED_AT/PRICE_ASC/PRICE_DESC | FAIL(최초) / 수정후PASS | |
| TC-023 | Product | Draft 조회 (GET /api/v1/products/drafts/{id}) | 200 OK, 상세 정보 반환 | PASS | 미수행 | - | PASS | |
| TC-024 | Product | Draft -> UNDER_REVIEW 전환 (POST /api/v1/products/drafts/{id}/submit) | 200 OK, status=UNDER_REVIEW | PASS ({"productId":1,"status":"UNDER_REVIEW",...}) | 미수행 | - | PASS | |
| TC-025 | Product | 내 상품 목록 (GET /api/v1/my-products) | 200 OK, Page 반환 | PASS (상품 1건 정상 조회) | FAIL: FE ENDPOINTS.PRODUCTS.MY_PRODUCTS = /api/v1/products/mine 이나 BE = /api/v1/my-products (경로 불일치) | CRITICAL: FE-BE URL 불일치로 내 상품 목록 조회 불가 | PASS(BE)/FAIL(FE) | FE 경로 오류 |
| TC-026 | Product | 상품 삭제 (DELETE /api/v1/my-products/{id}) | 204 No Content | 미수행 (UNDER_REVIEW 상태 상품 삭제 정책 불명확) | 미수행 | - | 미수행 | |
| **[Product - 대여자]** | | | | | | | | |
| TC-027 | Product | 상품 검색 (GET /api/v1/products) | 200 OK, Page 반환 | PASS (빈 목록 반환 - APPROVED 상품 없음) | FAIL: FE /products 라우트 404 | CRITICAL: FE ENDPOINTS.PRODUCTS.SEARCH = /api/v1/products/search, BE = /api/v1/products (쿼리파람). PRODUCTS.BASE는 맞으나 SEARCH가 별도 잘못된 경로 | PASS(BE)/FAIL(FE라우트) | FE 라우트 미등록 |
| TC-028 | Product | 상품 검색 - 필터/정렬 | 필터 파라미터 반영 | PASS (keyword/category/minPrice/maxPrice/sortBy 파라미터 수용) | 미수행 | MAJOR: FE sort 파라미터 "latest" 전송하나 BE ProductSortBy에 "latest" 없음 (CREATED_AT 사용해야 함) | PASS(BE)/FAIL(FE파라미터) | |
| TC-029 | Product | 상품 검색 - 페이지네이션 | Page 구조 반환 | PASS (content/pageable/totalElements 구조 정상) | 미수행 | MINOR: FE PaginatedResponse.hasNext 사용, BE Spring Page에 hasNext 없음 (last를 역으로 확인해야 함) | PASS(BE)/MINOR(FE) | Spring Page 구조 차이 |
| TC-030 | Product | 상품 상세 조회 (GET /api/v1/products/{id}) | 200 OK (APPROVED 상품만), 비승인은 소유자에게만 200 | PASS (owner=200, anonymous+UNDER_REVIEW=403) | FAIL (/products/1 라우트 404) | - | PASS(BE)/FAIL(FE라우트) | |
| TC-031 | Product | CategoryPriceGuide 조회 (GET /api/v1/price-guides) | 200 OK, 카테고리별 가격 가이드 | PASS (빈 배열 반환 - 시드 데이터 없음) | 미수행 | CRITICAL: FE ENDPOINTS.GUIDE_PRICES.BASE = /api/v1/guide-prices, BE = /api/v1/price-guides (URL 불일치) | PASS(BE)/FAIL(FE경로) | FE URL 오류 |
| **[Admin]** | | | | | | | | |
| TC-032 | Admin | 상품 승인 (PATCH /api/admin/products/{id}/approve) | 200 OK, 도메인 이벤트 발행 | FAIL: NPE - Product.domainEvents가 null (Product.kt:133) | 미수행 | - | FAIL | BLOCKER: NPE in Product.approve() |
| TC-033 | Admin | 상품 반려 (PATCH /api/admin/products/{id}/reject) | 200 OK, 반려 사유 저장 | FAIL: NPE - 동일 원인으로 추정 | 미수행 | - | FAIL | BLOCKER: NPE in Product.reject() |
| **[Notification]** | | | | | | | | |
| TC-034 | Notification | 알림 목록 조회 (GET /api/v1/notifications) | 200 OK, PageResult 반환 | PASS ({"content":[],"totalElements":0,"totalPages":0}) | PASS (FE /notifications 200, "알림을 불러오는 중..." 표시) | - | PASS | |
| TC-035 | Notification | 알림 읽음 처리 (PATCH /api/v1/notifications/{id}/read) | 200 OK | 미수행 (알림 데이터 없음) | 미수행 | - | 미수행 | 데이터 없어 검증 불가 |
| TC-036 | Notification | 전체 알림 읽음 (PATCH /api/v1/notifications/read-all) | 200 OK | PASS | 미수행 | - | PASS | |
| TC-037 | Notification | 미읽음 수 조회 (GET /api/v1/notifications/unread-count) | 200 OK, {"count": N} | PASS ({"count":0}) | 미수행 | - | PASS | |
| **[MinIO]** | | | | | | | | |
| TC-038 | MinIO | Presigned URL 생성 (POST /api/v1/images/presigned-url) | 200 OK, presignedUrl 반환 | FAIL: 500 - minio.rental-commerce.svc.cluster.local hostname 해석 실패 | 미수행 | BLOCKER: MinIO 엔드포인트가 잘못된 네임스페이스 DNS 사용 (rental-commerce vs rental-commerce-dev) | FAIL | BLOCKER: DNS 오류 |
| TC-039 | MinIO | Bucket 설정 일치 여부 | application.yml buckets 키가 MinIO 실제 버킷과 일치 | FAIL: application.yml = {default, product-images}, MinIO 실제 = {products, inspections, documents, chat} (불일치) | 미수행 | - | FAIL | CRITICAL |
| **[Infrastructure]** | | | | | | | | |
| TC-040 | Infra | Prometheus 메트릭 수집 | /actuator/prometheus 200, Prometheus ready | PASS (Prometheus ready + up metrics 확인) | N/A | - | PASS | |
| TC-041 | Infra | Grafana UI 접근 | Grafana health 200, version 확인 | PASS ({"database":"ok","version":"12.4.2"}) | N/A | - | PASS | |
| TC-042 | Infra | Pinpoint Web UI 접근 | Pinpoint web 200 OK | PASS (HTTP 200 HTML 반환) | N/A | - | PASS | pinpoint-hbase CrashLoopBackOff로 APM 데이터 없음 |
| TC-043 | Infra | pinpoint-hbase CrashLoopBackOff | Running 1/1 | FAIL: CrashLoopBackOff 상태 (18회 재시작) | N/A | - | FAIL | MAJOR: APM 데이터 저장 불가 |
| TC-044 | Infra | Actuator Health | /actuator/health 200 | FAIL (port-forward 18080 경유 actuator 미응답 - /actuator/health 무응답) | N/A | - | 미수행 | |
| TC-045 | Infra | API 경고 로그 (PageImpl 직렬화) | PageImpl 직렬화 경고 없음 | FAIL: WARN - Serializing PageImpl instances as-is is not supported (ProductSearchApiController) | N/A | - | FAIL | MINOR: Spring Data Web 설정 필요 |

---

## 테스트 계정 정보

| 역할 | 이메일 | 비밀번호 | userId |
|------|--------|----------|--------|
| LENDER | qa_lender@test.com | Test1234! | 1 |
| RENTER | qa_renter@test.com | Test1234! | 2 |

## 생성 데이터

| 항목 | ID | 상태 |
|------|----|------|
| Product #1 | 1 | UNDER_REVIEW (캠핑텐트 3인용) |
| LenderProfile | 1 | PENDING |
| RenterProfile | 1 | BRONZE (shippingAddress 설정됨) |
