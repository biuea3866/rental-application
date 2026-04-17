import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ────────────────────────────────────────────────────────────────────────────
// Custom metrics — 각 단계별 응답시간 측정
// ────────────────────────────────────────────────────────────────────────────
const loginDuration = new Trend('e2e_login_duration', true);
const productSearchDuration = new Trend('e2e_product_search_duration', true);
const productDetailDuration = new Trend('e2e_product_detail_duration', true);
const rentalRequestDuration = new Trend('e2e_rental_request_duration', true);
const paymentDuration = new Trend('e2e_payment_duration', true);
const e2eErrorRate = new Rate('e2e_error_rate');
const e2eFlowSuccessRate = new Rate('e2e_flow_success_rate');

export const options = {
  // 50 VU 동시 실행 — E2E 플로우 전체 시뮬레이션
  vus: 50,
  duration: '3m',
  thresholds: {
    // 전체 요청 p95 < 500ms
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    // 단계별 임계값
    e2e_login_duration: ['p(95)<300'],
    e2e_product_search_duration: ['p(95)<400'],
    e2e_product_detail_duration: ['p(95)<400'],
    e2e_rental_request_duration: ['p(95)<500'],
    e2e_payment_duration: ['p(95)<500'],
    // 전체 E2E 플로우 에러율 < 1%
    e2e_error_rate: ['rate<0.01'],
    e2e_flow_success_rate: ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트 계정 — 실제 환경에 맞게 조정 필요
// VU index 기반으로 분산하여 계정 충돌 방지
const TEST_USERS = Array.from({ length: 100 }, (_, i) => ({
  email: `test-user-${i + 1}@rental-test.com`,
  password: 'Test@1234!',
}));

function pickUser() {
  const idx = (__VU - 1) % TEST_USERS.length;
  return TEST_USERS[idx];
}

// ────────────────────────────────────────────────────────────────────────────
// Step 1 — 로그인
// ────────────────────────────────────────────────────────────────────────────
function stepLogin(user) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: user.email, password: user.password }),
    { headers: { 'Content-Type': 'application/json' }, timeout: '10s' },
  );

  loginDuration.add(res.timings.duration);

  const ok = check(res, {
    '[Login] status is 200': (r) => r.status === 200,
    '[Login] accessToken exists': (r) => {
      try {
        return JSON.parse(r.body).accessToken !== undefined;
      } catch (_) {
        return false;
      }
    },
  });

  if (!ok) return null;

  try {
    return JSON.parse(res.body).accessToken;
  } catch (_) {
    return null;
  }
}

// ────────────────────────────────────────────────────────────────────────────
// Step 2 — 상품 목록 조회 (공개 API)
// ────────────────────────────────────────────────────────────────────────────
function stepSearchProducts() {
  const res = http.get(
    `${BASE_URL}/api/v1/products?size=20&sortBy=CREATED_AT&sortDirection=DESC`,
    { headers: { 'Content-Type': 'application/json' }, timeout: '10s' },
  );

  productSearchDuration.add(res.timings.duration);

  const ok = check(res, {
    '[ProductSearch] status is 200': (r) => r.status === 200,
    '[ProductSearch] content exists': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.content !== undefined;
      } catch (_) {
        return false;
      }
    },
  });

  if (!ok) return null;

  try {
    const body = JSON.parse(res.body);
    const items = body.content;
    if (!items || items.length === 0) return null;
    return items[Math.floor(Math.random() * items.length)].productId;
  } catch (_) {
    return null;
  }
}

// ────────────────────────────────────────────────────────────────────────────
// Step 3 — 상품 상세 조회
// ────────────────────────────────────────────────────────────────────────────
function stepGetProductDetail(productId, token) {
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  const res = http.get(
    `${BASE_URL}/api/v1/products/${productId}`,
    { headers, timeout: '10s' },
  );

  productDetailDuration.add(res.timings.duration);

  return check(res, {
    '[ProductDetail] status is 200': (r) => r.status === 200,
    '[ProductDetail] productId matches': (r) => {
      try {
        return JSON.parse(r.body).productId === productId;
      } catch (_) {
        return false;
      }
    },
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Step 4 — 대여 신청
// ────────────────────────────────────────────────────────────────────────────
function stepRequestRental(productId, token) {
  const now = new Date();
  const startDate = new Date(now.getTime() + 24 * 60 * 60 * 1000);
  const endDate = new Date(startDate.getTime() + 3 * 24 * 60 * 60 * 1000);

  const payload = JSON.stringify({
    productId,
    startDate: startDate.toISOString(),
    endDate: endDate.toISOString(),
    dailyPrice: 10000,
    deliveryInfo: {
      recipientName: '홍길동',
      recipientPhone: '010-1234-5678',
      addressLine1: '서울특별시 강남구 테헤란로 123',
      addressLine2: '4층',
      zipCode: '06234',
    },
  });

  const res = http.post(
    `${BASE_URL}/api/v1/rentals`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      timeout: '10s',
    },
  );

  rentalRequestDuration.add(res.timings.duration);

  const ok = check(res, {
    '[RentalRequest] status is 201': (r) => r.status === 201,
    '[RentalRequest] rentalId exists': (r) => {
      try {
        return JSON.parse(r.body).rentalId !== undefined;
      } catch (_) {
        return false;
      }
    },
  });

  if (!ok) return null;

  try {
    return JSON.parse(res.body).rentalId;
  } catch (_) {
    return null;
  }
}

// ────────────────────────────────────────────────────────────────────────────
// Step 5 — 결제 처리
// ────────────────────────────────────────────────────────────────────────────
function stepProcessPayment(rentalId, token) {
  const payload = JSON.stringify({
    paymentKey: `test-payment-key-${rentalId}-${Date.now()}`,
    amount: 30000,
    paymentMethod: 'CARD',
  });

  const res = http.post(
    `${BASE_URL}/api/v1/rentals/${rentalId}/payment`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      timeout: '10s',
    },
  );

  paymentDuration.add(res.timings.duration);

  return check(res, {
    '[Payment] status is 200': (r) => r.status === 200,
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Main E2E flow
// ────────────────────────────────────────────────────────────────────────────
export default function () {
  let flowSuccess = false;

  group('E2E Rental Flow', () => {
    const user = pickUser();

    // Step 1: 로그인
    const token = group('Step1: Login', () => stepLogin(user));
    if (!token) {
      e2eErrorRate.add(true);
      e2eFlowSuccessRate.add(false);
      return;
    }
    sleep(0.5);

    // Step 2: 상품 목록 조회
    const productId = group('Step2: Search Products', () => stepSearchProducts());
    if (!productId) {
      e2eErrorRate.add(true);
      e2eFlowSuccessRate.add(false);
      return;
    }
    sleep(0.5);

    // Step 3: 상품 상세 조회
    const detailOk = group('Step3: Get Product Detail', () =>
      stepGetProductDetail(productId, token),
    );
    if (!detailOk) {
      e2eErrorRate.add(true);
      e2eFlowSuccessRate.add(false);
      return;
    }
    sleep(0.5);

    // Step 4: 대여 신청
    const rentalId = group('Step4: Request Rental', () =>
      stepRequestRental(productId, token),
    );
    if (!rentalId) {
      e2eErrorRate.add(true);
      e2eFlowSuccessRate.add(false);
      return;
    }
    sleep(0.5);

    // Step 5: 결제
    const payOk = group('Step5: Process Payment', () =>
      stepProcessPayment(rentalId, token),
    );

    flowSuccess = payOk;
    e2eErrorRate.add(!flowSuccess);
    e2eFlowSuccessRate.add(flowSuccess);
  });

  sleep(1);
}
