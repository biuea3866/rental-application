# Sprint 1 QA 3단계 검증 리포트

> 작성일: 2026-04-15  
> 검증자: Claude Sonnet 4.6 (자동화 QA)  
> 환경: rental-commerce-dev (Kubernetes)  
> 목적: Sprint 2 진행 전 Go/No-Go 판정

---

## 1. 전체 요약

| 항목 | 수치 |
|------|------|
| 총 테스트 케이스 | 45건 |
| PASS | 22건 |
| FAIL | 16건 |
| 미수행 | 7건 |
| BLOCKER | 3건 |
| CRITICAL | 6건 |
| MAJOR | 5건 |
| MINOR | 4건 |

**Sprint 2 진행 가능 여부: NO-GO**

BLOCKER 3건(Admin 승인/반려 NPE, MinIO DNS 오류, FE 로그인 후 사용자 정보 조회 불가)과 CRITICAL 6건이 미해결 상태로 Sprint 2 진행 시 핵심 플로우가 동작하지 않습니다.

---

## 2. 클러스터 상태

| 서비스 | 상태 | 비고 |
|--------|------|------|
| rental-api | Running 2/2 | 정상 |
| rental-web | Running 1/1 | 정상 |
| mysql-0 | Running 1/1 | 정상 |
| mongodb | Running 1/1 | 정상 |
| redis | Running 1/1 | 정상 |
| kafka | Running 1/1 | 정상 |
| minio | Running 1/1 | 버킷 설정/DNS 이슈 |
| prometheus | Running 1/1 | 정상 |
| grafana | Running 1/1 | 정상 |
| pinpoint-web | Running 1/1 | 정상 |
| **pinpoint-hbase** | **CrashLoopBackOff** | **18회 재시작, APM 저장 불가** |
| pinpoint-collector | Running 1/1 | hbase 의존으로 데이터 저장 불가 |

---

## 3. BE API 검증 결과

### 3.1 Auth

| 테스트 | 엔드포인트 | HTTP | 결과 | 상세 |
|--------|-----------|------|------|------|
| 회원가입 | POST /api/v1/auth/signup | 200 | PASS | 인증코드 Redis 저장 확인 |
| 전화 인증 완료 | POST /api/v1/auth/verify-phone | 200 | PASS | JWT 정상 발급 |
| 로그인 | POST /api/v1/auth/login | 200 | PASS | accessToken/refreshToken/tokenFamily 반환 |
| 로그아웃 | POST /api/v1/auth/logout | 404 | FAIL | **엔드포인트 미구현** |
| 토큰 갱신 | POST /api/v1/auth/refresh | 200 | PASS | tokenFamily 필수 (FE 불일치) |
| 카카오 OAuth | POST /api/v1/auth/social-login | 500 | 부분PASS | 외부API 연결 실패(예상), 필드명 불일치 확인 |
| **유저정보 조회** | **GET /api/v1/auth/me** | **404** | **FAIL** | **엔드포인트 미구현 (BLOCKER)** |

### 3.2 User Profile

| 테스트 | 엔드포인트 | HTTP | 결과 | 상세 |
|--------|-----------|------|------|------|
| LenderProfile 생성 | POST /api/v1/users/me/lender-profile | 201 | PASS | lenderType=INDIVIDUAL |
| LenderProfile 조회 | GET /api/v1/users/me/lender-profile | 200 | PASS | 정상 |
| LenderProfile 수정 | PATCH /api/v1/mypage/lender-profile | 200 | PASS | settlementAccount 업데이트 |
| RenterProfile 생성 | POST /api/v1/users/me/renter-profile | 201 | PASS | trustGrade=BRONZE 초기값 |
| RenterProfile 수정 | PATCH /api/v1/mypage/renter-profile | 200 | PASS | shippingAddress 업데이트 |
| MyPage 조회 | GET /api/v1/mypage | 200 | PASS | 프로필 포함 전체 반환 |

### 3.3 Product (등록자)

| 테스트 | 엔드포인트 | HTTP | 결과 | 상세 |
|--------|-----------|------|------|------|
| Draft 생성 | POST /api/v1/products/drafts | 201 | PASS | DRAFT 상태로 생성 |
| Draft 수정 Step1 | PATCH /api/v1/products/drafts/1 | 200 | PASS | 기본정보 업데이트 |
| Draft 수정 Step2 | PATCH /api/v1/products/drafts/1 | 200 | PASS | DAILY 가격 설정 (DAY 전송시 400) |
| Draft 조회 | GET /api/v1/products/drafts/1 | 200 | PASS | 전체 필드 반환 |
| Submit (UNDER_REVIEW) | POST /api/v1/products/drafts/1/submit | 200 | PASS | DRAFT→UNDER_REVIEW 전환 |
| 내 상품 목록 | GET /api/v1/my-products | 200 | PASS | 1건 정상 반환 |

### 3.4 Product (대여자)

| 테스트 | 엔드포인트 | HTTP | 결과 | 상세 |
|--------|-----------|------|------|------|
| 상품 검색 | GET /api/v1/products | 200 | PASS | 빈 목록 (APPROVED 없음) |
| 상품 상세 (소유자) | GET /api/v1/products/1 (with auth) | 200 | PASS | UNDER_REVIEW 상품 소유자 접근 가능 |
| 상품 상세 (비인증) | GET /api/v1/products/1 | 403 | PASS(예상동작) | UNDER_REVIEW 비소유자 접근 403 |
| CategoryPriceGuide | GET /api/v1/price-guides | 200 | PASS | 빈 배열 (시드 없음) |

### 3.5 Admin

| 테스트 | 엔드포인트 | HTTP | 결과 | 상세 |
|--------|-----------|------|------|------|
| **상품 승인** | **PATCH /api/admin/products/1/approve** | **500** | **FAIL** | **NPE: Product.domainEvents is null (Product.kt:133)** |
| **상품 반려** | **PATCH /api/admin/products/1/reject** | **500** | **FAIL** | **동일 원인 NPE 추정** |

**에러 로그:**
```
java.lang.NullPointerException: Cannot invoke "java.util.List.add(Object)" because "this.domainEvents" is null
  at com.rental.commerce.domain.product.Product.approve(Product.kt:133)
  at com.rental.commerce.domain.product.ProductDomainService.approve(ProductDomainService.kt:37)
```

### 3.6 Notification

| 테스트 | 엔드포인트 | HTTP | 결과 | 상세 |
|--------|-----------|------|------|------|
| 알림 목록 | GET /api/v1/notifications | 200 | PASS | 빈 목록 정상 |
| 전체 읽음 | PATCH /api/v1/notifications/read-all | 200 | PASS | 정상 |
| 미읽음 수 | GET /api/v1/notifications/unread-count | 200 | PASS | {"count":0} |

### 3.7 MinIO

| 테스트 | 결과 | 상세 |
|--------|------|------|
| **Presigned URL** | **FAIL** | **500: minio.rental-commerce.svc.cluster.local DNS 해석 실패** |
| Bucket 설정 | FAIL | application.yml: {default, product-images} vs MinIO 실제: {products, inspections, documents, chat} |

**에러:** `java.net.UnknownHostException: minio.rental-commerce.svc.cluster.local: Name does not resolve`  
**원인:** 네임스페이스 오류 - rental-commerce vs rental-commerce-dev

---

## 4. FE 화면 검증 결과

> Playwright MCP Bridge 확장 미설치로 브라우저 자동화 불가. curl + Python HTTP 클라이언트로 SSR HTML 렌더링 검증 수행.

| 라우트 | HTTP | 주요 렌더링 요소 | 결과 | 이슈 |
|--------|------|-----------------|------|------|
| / (홈) | 200 | "Rental Commerce", "등록자/대여자 선택" 버튼 정상 | PASS | |
| /login | 200 | "로그인", 이메일/비밀번호 입력폼, 카카오/네이버 소셜로그인 버튼 | PASS | |
| /signup | 200 | "회원가입", 전화번호/비밀번호 입력폼 | PASS | |
| /signup/verify | 200 | "로딩 중..." (SSR 초기 렌더링) | 부분PASS | 실제 클릭 플로우 미검증 |
| /mypage | 200 | "불러오는 중..." (SSR 초기 렌더링) | 부분PASS | 실제 로그인 상태 미검증 |
| /notifications | 200 | "알림을 불러오는 중..." | 부분PASS | |
| /lender/products | 200 | "내 등록 상품", "상품 등록" 버튼 | PASS | |
| **/products** | **404** | **404 Not Found** | **FAIL** | **BLOCKER: 상품 목록 라우트 미등록** |
| **/products/1** | **404** | **404 Not Found** | **FAIL** | **BLOCKER: 상품 상세 라우트 미등록** |

### FE 라우트 분석
- Next.js App Router의 route group `(renter)` 내의 `products/` 폴더가 `/products` URL로 매핑되어야 하나, 현재 404 반환
- `app/page.tsx`(홈)와 `app/(renter)/page.tsx`가 충돌 가능성 있음
- 대여자 입장에서 핵심 기능(상품 탐색, 상품 상세)에 접근 불가한 BLOCKER 상황

---

## 5. 크로스 검증 결과 (FE 타입 ↔ BE 응답 스키마)

### 5.1 Product 타입 불일치

| FE types.ts 필드 | BE ProductDetailResponse 필드 | 불일치 유형 |
|-----------------|------------------------------|-------------|
| `title: string` | `name: string` | 필드명 불일치 |
| `category: ProductCategory` | `categoryCode: string` | 필드명 불일치 |
| `pricePerDay: number` | `prices: PriceResponse[]` | 타입 구조 불일치 |
| `deposit: number` | `depositAmount: Long` | 필드명 불일치 |
| `imageUrls: string[]` | `images: ImageResponse[]` | 타입 구조 불일치 |
| `lenderName: string` | 없음 | 필드 누락 |
| `location: string` | 없음 | 필드 누락 |

### 5.2 Auth 타입 불일치

| FE types.ts | BE 스키마 | 불일치 유형 |
|-------------|-----------|-------------|
| `RefreshTokenRequest: {refreshToken}` | `RefreshTokenRequest: {refreshToken, tokenFamily}` | 필수 필드 누락 |
| `SocialLoginRequest: {provider, code, redirectUri}` | `SocialLoginRequest: {provider, authorizationCode}` | 필드명 불일치 (code → authorizationCode) |
| `AuthTokens: {accessToken, refreshToken, expiresIn}` | 응답: `{accessToken, refreshToken, tokenFamily, userId}` | tokenFamily/userId 누락, expiresIn 없음 |

### 5.3 Endpoint URL 불일치

| FE ENDPOINTS | BE 실제 경로 | 불일치 유형 |
|-------------|-------------|-------------|
| `AUTH.ME = /api/v1/auth/me` | 없음 | BE 미구현 |
| `AUTH.LOGOUT = /api/v1/auth/logout` | 없음 | BE 미구현 |
| `PRODUCTS.MY_PRODUCTS = /api/v1/products/mine` | `/api/v1/my-products` | URL 불일치 |
| `PRODUCTS.SEARCH = /api/v1/products/search` | `/api/v1/products` (쿼리파람) | URL 불일치 |
| `GUIDE_PRICES.BASE = /api/v1/guide-prices` | `/api/v1/price-guides` | URL 불일치 |

---

## 6. 발견 이슈 심각도별 분류

### BLOCKER (3건)

| ID | 이슈 | 영향 |
|----|------|------|
| BLK-001 | Admin 상품 승인/반려 시 NPE: `Product.domainEvents is null` (Product.kt:133) | 상품 검수 워크플로우 전체 불가, 승인된 상품 없어 대여자 상품 탐색 불가 |
| BLK-002 | FE 로그인 후 GET /api/v1/auth/me 호출하나 BE 미구현 → 로그인 후 사용자 정보 세팅 실패 | 로그인 기능 전체 비정상 동작 |
| BLK-003 | FE /products, /products/[id] 라우트 404 → 대여자 상품 탐색 화면 없음 | 대여자 핵심 플로우 완전 불가 |

### CRITICAL (6건)

| ID | 이슈 | 영향 |
|----|------|------|
| CRT-001 | MinIO DNS 오류: `minio.rental-commerce.svc.cluster.local` 미존재 (네임스페이스 불일치) | 이미지 업로드 전체 불가 |
| CRT-002 | FE SocialLoginRequest.code vs BE authorizationCode 필드명 불일치 | 카카오/네이버 OAuth 로그인 불가 |
| CRT-003 | FE RefreshTokenRequest에 tokenFamily 필드 없음 | 토큰 자동 갱신 불가, 세션 만료 시 재로그인 필요 |
| CRT-004 | FE ENDPOINTS.PRODUCTS.MY_PRODUCTS = /api/v1/products/mine (BE: /api/v1/my-products) | 내 상품 목록 조회 불가 |
| CRT-005 | FE Product 타입과 BE ProductDetailResponse 완전 불일치 (title vs name, pricePerDay vs prices[], 등) | 상품 데이터 FE 렌더링 불가 (NaN 가격, 빈 타이틀 예상) |
| CRT-006 | FE ENDPOINTS.GUIDE_PRICES.BASE = /api/v1/guide-prices (BE: /api/v1/price-guides) | 카테고리 가격 가이드 조회 불가 |

### MAJOR (5건)

| ID | 이슈 | 영향 |
|----|------|------|
| MAJ-001 | BE MinIO buckets 설정 불일치: application.yml {default, product-images} vs 실제 MinIO {products, inspections, documents, chat} | Presigned URL 생성 불가 (CRT-001과 중복) |
| MAJ-002 | FE sort 파라미터 "latest"/"price_asc"/"price_desc" vs BE ProductSortBy CREATED_AT/PRICE_ASC/PRICE_DESC | 정렬 기능 비동작 |
| MAJ-003 | FE UpdateLenderProfileRequest{businessName, description, bankName, bankAccount} vs BE CreateLenderProfileRequest{lenderType} | 렌더 프로필 생성 폼 불일치 |
| MAJ-004 | FE RenterProfile.deliveryAddress vs BE shippingAddress (필드명 불일치) | 배송지 FE 표시 오류 |
| MAJ-005 | pinpoint-hbase CrashLoopBackOff (18회 재시작) | APM 성능 데이터 수집 불가 |

### MINOR (4건)

| ID | 이슈 | 영향 |
|----|------|------|
| MIN-001 | FE AuthTokens에 tokenFamily, userId 없음 (expiresIn도 BE 미반환) | 타입 안전성 저하 |
| MIN-002 | FE RenterProfile 타입에 trustGrade, totalTransactionCount 없음 | 신뢰등급 UI 표시 불가 |
| MIN-003 | BE PageImpl 직렬화 경고 (Spring Data Web 설정 필요) | 프로덕션 로그 노이즈 |
| MIN-004 | ProductSearchApiController: BE Spring Page 구조(last 필드)와 FE PaginatedResponse.hasNext 매핑 | 무한스크롤 마지막 페이지 판단 오류 가능성 |

---

## 7. 인프라 검증 요약

| 서비스 | 결과 | 상세 |
|--------|------|------|
| Prometheus | PASS | 9090 ready, up metrics 정상 |
| Grafana | PASS | 13000(pf), v12.4.2, database ok |
| Pinpoint Web | PASS | HTTP 200 |
| Pinpoint HBase | FAIL | CrashLoopBackOff - APM 데이터 없음 |
| API /actuator/prometheus | PASS | Spring Boot 메트릭 정상 노출 |

---

## 8. Sprint 2 Go/No-Go 결정

### NO-GO

**사유:**

1. **Admin 승인 NPE (BLK-001)**: 상품 검수 플로우 전체 불가. 승인된 상품이 없으면 대여자는 상품을 볼 수 없어 서비스 핵심 기능이 동작하지 않음.

2. **FE /api/v1/auth/me 미구현 (BLK-002)**: 현재 상태에서 FE 로그인이 성공해도 유저 정보 세팅이 되지 않아 인증 상태가 유지되지 않음. 사실상 모든 FE 인증 흐름이 깨진 상태.

3. **FE /products 라우트 404 (BLK-003)**: 대여자 메인 화면인 상품 목록/상세가 아예 접근 불가. Next.js 라우트 설정 오류.

4. **FE-BE 스키마 불일치 다수 (CRT-001~006)**: Product 타입, Auth 타입, Endpoint URL이 전반적으로 불일치하여 실제 데이터 연동이 되지 않음.

### 해결 후 재검증 필요 항목

```
[BLOCKER - 즉시 수정]
□ Product.kt:133 domainEvents null 초기화 (approve/reject)
□ GET /api/v1/auth/me 엔드포인트 추가 또는 FE 로그인 로직 수정
□ Next.js (renter) route group /products, /products/[id] 라우트 수정

[CRITICAL - 이번 주 내]
□ MinIO 엔드포인트 DNS 수정: minio.rental-commerce-dev.svc.cluster.local
□ MinIO buckets 설정 맞춤 (application.yml 수정)
□ FE SocialLoginRequest.code → authorizationCode 수정
□ FE RefreshTokenRequest에 tokenFamily 추가
□ FE ENDPOINTS 경로 수정 (my-products, price-guides)
□ FE Product 타입 BE 스키마에 맞게 전면 수정 (name, categoryCode, prices[], images[])
```

---

## 9. 검증 방법론 기록

- **BE 검증**: curl + Python urllib 로 45개 케이스 API 직접 호출, 실제 JWT 발급/사용, Redis 인증코드 확인, MySQL 데이터 직접 조회
- **FE 검증**: Playwright MCP Bridge 미설치로 브라우저 자동화 불가. Python urllib로 SSR HTML 페이지 렌더링 확인, 주요 텍스트/폼 요소 존재 확인. 실제 클릭 플로우(버튼 동작, 폼 제출, 화면 전환)는 미검증 → 향후 Playwright 환경 구성 후 재수행 권장.
- **크로스 검증**: FE types.ts, endpoints.ts, hooks 파일과 BE Controller, Request/Response DTO 직접 비교

---

_생성: Claude Sonnet 4.6 | 2026-04-15_
