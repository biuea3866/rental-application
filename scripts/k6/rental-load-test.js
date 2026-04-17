import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('rental_error_rate');
const rentalDuration = new Trend('rental_request_duration', true);

export const options = {
  stages: [
    // Ramp-up: 0 → 500 VU over 30s
    { duration: '30s', target: 500 },
    // Sustained load: 500 VU for 2m
    { duration: '2m', target: 500 },
    // Ramp-down: 500 → 0 VU over 30s
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    // p95 응답시간 500ms 미만
    http_req_duration: ['p(95)<500'],
    // 에러율 1% 미만
    http_req_failed: ['rate<0.01'],
    // 커스텀 메트릭 임계값
    rental_request_duration: ['p(95)<500'],
    rental_error_rate: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트 데이터 — 실제 환경에 맞게 조정 필요
const PRODUCT_IDS = [1, 2, 3, 4, 5, 10, 15, 20, 25, 30];

function randomProductId() {
  return PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
}

function buildRentalPayload() {
  const now = new Date();
  const startDate = new Date(now.getTime() + 24 * 60 * 60 * 1000); // tomorrow
  const endDate = new Date(startDate.getTime() + 3 * 24 * 60 * 60 * 1000); // +3 days

  return JSON.stringify({
    productId: randomProductId(),
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
}

export default function () {
  const memberId = Math.floor(Math.random() * 1000) + 1;

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Member-Id': String(memberId),
    },
    timeout: '10s',
  };

  const res = http.post(
    `${BASE_URL}/api/v1/rentals`,
    buildRentalPayload(),
    params,
  );

  // k6 engine이 측정한 정확한 네트워크 왕복 시간 사용 (Date.now() 오버헤드 제거)
  rentalDuration.add(res.timings.duration);

  const success = check(res, {
    'status is 201 (created)': (r) => r.status === 201,
    'response body is not empty': (r) => r.body && r.body.length > 0,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!success);

  sleep(1);
}
