# ADR-004: 모니터링 / 오브젝트 스토리지 / 부하 테스트

- **Status**: Accepted
- **Date**: 2026-04-11

## Context

서비스 운영에 필요한 APM 모니터링, 이미지/파일 저장소, 실제 유저 시나리오 기반 부하 테스트 전략을 정의해야 한다.

## Decision

### 1. APM 모니터링 — Pinpoint

```
구성:
  - Pinpoint Agent: 애플리케이션 JVM에 attach
  - Pinpoint Collector: 트레이스 데이터 수집
  - Pinpoint Web: 대시보드 UI
  - HBase: 트레이스 데이터 저장

모니터링 대상:
  - API 응답 시간 / 처리량 / 에러율
  - 도메인 서비스 간 호출 트레이스
  - MySQL 쿼리 실행 시간
  - Kafka Producer/Consumer 지연
  - Redis 캐시 hit/miss
  - 외부 API (PG, 택배, 알림톡) 응답 시간

알림 기준:
  - API P99 > 3초
  - 에러율 > 1%
  - Kafka Consumer lag > 1000
```

### 2. 오브젝트 스토리지 — MinIO (S3 호환)

```
용도:
  - 상품 이미지 업로드/조회
  - 반납 검수 사진
  - 사업자등록증 업로드
  - 채팅 이미지

구성:
  - MinIO 서버 (Docker, S3 API 호환)
  - 버킷 구조:
    products/          — 상품 이미지
    inspections/       — 검수 사진
    documents/         — 사업자등록증 등
    chat/              — 채팅 이미지

  - 추후 AWS S3 마이그레이션 시 Gateway 구현체만 교체

인터페이스:
  domain/common/ObjectStorageGateway.kt     — Port (interface)
  infrastructure/common/minio/MinioGateway.kt — Adapter (구현체)
```

### 3. 부하 테스트 — k6

```
전략: 실제 유저 시나리오를 k6 스크립트로 작성, CI/지속적 실행

시나리오 목록:
  S01. 대여자 회원가입 → 상품 검색 → 대여 신청 → 결제
  S02. 등록자 회원가입 → 상품 등록 → 대여 수락 → 정산 확인
  S03. 대여 종료 → 반납 수거 → 검수 → 보증금 환급
  S04. 연체 시나리오 → 유예 → 연체료 과금
  S05. 리뷰 작성 → 등급 승급
  S06. 1:1 채팅 (WebSocket)
  S07. 동시 대여 신청 (동시성 경합)
  S08. 피처 플래그 비율 롤아웃 검증
  S09. 등록자 구독 플랜 업그레이드 → 수수료율 변경 확인
  S10. 상품 검색 필터 + 페이지네이션 부하

부하 프로파일:
  smoke:    VU=5,    duration=1m    — 기본 정상 동작 확인
  load:     VU=50,   duration=5m    — 일반 트래픽
  stress:   VU=200,  duration=10m   — 피크 트래픽
  soak:     VU=30,   duration=30m   — 장시간 안정성

성공 기준:
  - P95 응답 시간 < 500ms
  - P99 응답 시간 < 3000ms
  - 에러율 < 0.1%
  - 초당 처리량(RPS) > 100 (load 기준)
```

## 패키지 구조 변경

```
domain/common/
├── DomainEvent.kt
├── BaseEntity.kt
└── ObjectStorageGateway.kt          — 파일 업로드/다운로드 Port

infrastructure/common/
├── event/
│   ├── FailedEvent.kt
│   ├── FailedEventRepository.kt
│   └── EventRetryHandler.kt
├── redis/
│   └── DistributedLockClient.kt
└── minio/
    └── MinioGatewayImpl.kt          — S3 호환 구현체

k6/
├── scenarios/
│   ├── s01-renter-rental-flow.js
│   ├── s02-lender-product-flow.js
│   ├── s03-return-inspection-flow.js
│   ├── s04-overdue-scenario.js
│   ├── s05-review-grade-flow.js
│   ├── s06-chat-websocket.js
│   ├── s07-concurrent-rental.js
│   ├── s08-feature-flag-rollout.js
│   ├── s09-subscription-upgrade.js
│   └── s10-search-pagination.js
├── profiles/
│   ├── smoke.js
│   ├── load.js
│   ├── stress.js
│   └── soak.js
├── helpers/
│   ├── auth.js                      — 로그인/토큰 헬퍼
│   ├── data-generator.js            — 테스트 데이터 생성
│   └── assertions.js                — 공통 검증
└── config.js                        — 환경별 설정
```

## Consequences

### 장점
- Pinpoint: 코드 수정 없이 Agent만으로 전체 트레이스 확보
- MinIO: S3 API 호환으로 추후 AWS 마이그레이션 무비용
- k6: 시나리오 기반 테스트로 실제 유저 패턴 재현, CI 통합 가능

### 단점/리스크
- Pinpoint: HBase 운영 부담 → Docker Compose로 로컬 구성
- MinIO: 단일 노드 한계 → 프로덕션 시 AWS S3로 전환
- k6: WebSocket 시나리오 작성 복잡도 → 기본 HTTP 시나리오부터 시작
