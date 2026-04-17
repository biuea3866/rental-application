# k6 부하 테스트 — Rental Commerce

이 디렉토리에는 Rental Commerce API에 대한 k6 부하 테스트 스크립트가 포함되어 있습니다.

---

## 사전 요구 사항

```bash
# macOS
brew install k6

# Docker
docker pull grafana/k6
```

---

## 스크립트 목록

| 파일 | 목적 | VU | 소요 시간 |
|------|------|----|-----------|
| `rental-load-test.js` | 대여 신청 단일 엔드포인트 동시성 부하 테스트 | 500 | ~3분 |
| `rental-e2e-load-test.js` | 로그인→상품조회→대여신청→결제 전체 E2E 플로우 부하 테스트 | 50 | ~3분 |

---

## 실행 방법

### 로컬 서버 대상 (기본값: `http://localhost:8080`)

```bash
# 대여 신청 부하 테스트
k6 run scripts/k6/rental-load-test.js

# E2E 플로우 부하 테스트
k6 run scripts/k6/rental-e2e-load-test.js
```

### 환경 변수로 대상 서버 지정

```bash
# dev 서버 대상
k6 run -e BASE_URL=http://dev.rental-commerce.internal:8080 scripts/k6/rental-load-test.js

# Docker 실행
docker run --rm -i grafana/k6 run - <scripts/k6/rental-load-test.js
```

### 결과를 JSON 파일로 저장

```bash
k6 run --out json=results/rental-load-test-$(date +%Y%m%d%H%M%S).json scripts/k6/rental-load-test.js
```

---

## 성공 기준 (Thresholds)

| 지표 | 임계값 | 설명 |
|------|--------|------|
| `http_req_duration p(95)` | < 500ms | 95번째 백분위 응답시간 |
| `http_req_failed` | < 1% | HTTP 에러율 |
| `e2e_login_duration p(95)` | < 300ms | 로그인 응답시간 (E2E) |
| `e2e_product_search_duration p(95)` | < 400ms | 상품 검색 응답시간 (E2E) |
| `e2e_rental_request_duration p(95)` | < 500ms | 대여 신청 응답시간 (E2E) |
| `e2e_flow_success_rate` | > 99% | 전체 E2E 플로우 성공률 |

---

## rental-load-test.js — 대여 신청 부하 테스트

### 시나리오

```
VU 수
500 |               ██████████████████████
    |           ████                      ████
  0 +─────────────────────────────────────────── 시간
        0s     30s              2m30s    3m
```

- **Ramp-up** (0~30s): 0 → 500 VU 점진적 증가
- **Sustained** (30s~2m30s): 500 VU 유지 (최대 부하)
- **Ramp-down** (2m30s~3m): 500 → 0 VU 점진적 감소

### 엔드포인트

```
POST /api/v1/rentals
X-Member-Id: {random 1~1000}
Content-Type: application/json

{
  "productId": {random},
  "startDate": "{tomorrow ISO8601}",
  "endDate": "{tomorrow+3days ISO8601}",
  "dailyPrice": 10000,
  "deliveryInfo": { ... }
}
```

### 응답 성공 조건

- HTTP 201 Created
- 응답 바디 비어있지 않음
- 응답시간 < 500ms

---

## rental-e2e-load-test.js — E2E 플로우 부하 테스트

### 시나리오

50 VU가 동시에 아래 플로우를 반복 실행합니다:

```
Step 1: POST /api/v1/auth/login
        └─ accessToken 획득
Step 2: GET  /api/v1/products?size=20
        └─ productId 획득
Step 3: GET  /api/v1/products/{productId}
        └─ 상품 상세 확인
Step 4: POST /api/v1/rentals
        └─ rentalId 획득
Step 5: POST /api/v1/rentals/{rentalId}/payment
        └─ 결제 완료
```

각 단계 사이에 0.5s sleep, 플로우 완료 후 1s sleep.

### 테스트 계정 설정

`TEST_USERS` 배열은 100개의 테스트 계정을 순환합니다.
실제 테스트 실행 전 DB에 `test-user-1@rental-test.com` ~ `test-user-100@rental-test.com` 계정을 생성해야 합니다.

```bash
# Flyway seed 또는 직접 삽입 예시 (dev 환경)
./gradlew :rental-infrastructure:flywayMigrate -Pflyway.locations=classpath:db/migration,classpath:db/testdata
```

---

## 결과 해석 가이드

### 콘솔 출력 예시

```
✓ status is 201 (created) ................: 99.80% ✓ 49900 ✗ 100
✓ response time < 500ms ..................: 96.20% ✓ 48100 ✗ 1900

http_req_duration.............: avg=120ms min=45ms med=105ms max=892ms p(90)=210ms p(95)=310ms p(99)=520ms
http_req_failed...............: 0.20%  ✓ 100    ✗ 49900
```

### 주요 지표 해석

| 지표 | 정상 범위 | 경고 | 조치 |
|------|-----------|------|------|
| `p(95) < 500ms` | PASS | 500~1000ms | 쿼리 최적화, 인덱스 검토 |
| `error rate < 1%` | PASS | 1~5% | 에러 로그 확인, DB 커넥션 풀 점검 |
| `p(99) < 1000ms` | 권장 | 1000ms 초과 | 타임아웃 설정, 캐시 적용 검토 |

### Threshold FAIL 시 확인 사항

1. **응답시간 초과**: `WARN: Application 레이어 N+1 쿼리 가능성 — QueryDSL fetchJoin 적용 여부 확인`
2. **에러율 초과**: `ERROR: 서버 에러 — Spring Boot actuator /health 확인, DB 커넥션 풀 포화 점검`
3. **E2E 플로우 실패**: 각 단계별 커스텀 메트릭(`e2e_*_duration`) 개별 확인

---

## CI/CD 연동 (GitHub Actions)

`.github/workflows/k6-load-test.yml` 참고.

```yaml
- name: Run k6 load test
  uses: grafana/k6-action@v0.3.1
  with:
    filename: scripts/k6/rental-load-test.js
  env:
    BASE_URL: ${{ secrets.DEV_SERVER_URL }}
```

---

## 관련 티켓

- Jira: `RC-DEVOPS-213`
- PR: `feature/RC-DEVOPS-213`
