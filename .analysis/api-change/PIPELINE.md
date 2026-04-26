# API 변경 분석 파이프라인

## 목적
공개/내부 API 변경의 소비자(FE/타 서비스/외부) 영향을 계약 관점에서 분석. 호환성 전략(expand/contract, versioning, deprecation) 결정.

## 담당 에이전트

| 역할 | 에이전트 |
|---|---|
| 오케스트레이션 | pipeline-runner |
| BE 계약 | be-tech-lead |
| FE 영향 | fe-lead |
| 운영/롤아웃 | be-senior |

## 입력
- 대상 API (REST / gRPC / GraphQL / Kafka topic / DB view)
- 변경 유형 (필드 추가/제거/타입/필수성/경로/응답코드)
- 소비자 목록
- 배포 창

## 단계

### 1. Breaking vs Non-breaking

**Non-breaking**:
- 새 optional 필드
- 새 엔드포인트
- 응답에 필드 추가 (소비자 unknown 무시)
- 응답 enum에 새 값 (default 처리)

**Breaking**:
- 필드 제거
- 타입 변경
- 필수 필드 추가 (요청)
- 응답 필드 제거
- enum 값 제거
- 경로/메서드 변경
- 상태 코드 변경

Kafka Avro: Full/Backward/Forward 호환성 명시.

### 2. 소비자 매핑
- 코드 검색 (호출 저장소)
- 모니터링 (실제 호출 user-agent/service)
- 외부 소비자: API 문서 공지 기록 대조
- 각 소비자 오너 + 변경 공수

### 3. 호환성 전략

| 상황 | 전략 |
|---|---|
| 내부 1 소비자 | PR 쌍 동시 (producer expand → consumer 이동 → producer contract) |
| 내부 N 소비자 | 버전 공존 + 마이그레이션 기간 |
| 외부 소비자 | 버전 공존 + 3개월 deprecation + 공지 |
| Kafka 스키마 | Backward 필수. 불가 시 새 토픽 + 이중 발행 |
| DB view | 컬럼 제거는 contract 단계 분리 |

### 4. 롤아웃
- **Expand**: 새 필드/엔드포인트/버전 추가 (기존 유지)
- **Migrate**: 소비자 이동 (PR 타임라인)
- **Contract**: 기존 제거 (관찰 기간 후)

각 단계 간 최소 관찰 기간 기록.

### 5. Deprecation 공지
- 외부: `Deprecation: true`, `Sunset: <date>` 헤더
- 내부: Slack + Confluence + `@Deprecated`
- OpenAPI 스펙 업데이트
- 호출량 모니터링

### 6. 검증
- 계약 테스트 (Pact/Spring Cloud Contract)
- OpenAPI diff (oasdiff)
- 소비자 staging 검증

## 산출물

```markdown
# API Change: <endpoint or topic>

**Type:** BREAKING

## 변경
- API: `POST /v1/orders`
- 변경: `phoneNumber` string → object({code, number})
- 동기: 국제화

## 분류
**BREAKING** — 타입 변경

## 소비자
| 소비자 | 타입 | 오너 | 공수 | 상태 |
|---|---|---|---|---|

## 전략
v1 유지 + v2 추가, 3개월 공존
- Expand: `POST /v2/orders`
- Migrate: FE/모바일 순차 (~6월 말)
- Contract: 2026-08-01 v1 제거

## Avro/계약
- Schema registry: BACKWARD
- 충돌 없음

## 타임라인
- 2026-04-26: v2 배포
- 2026-04-26: 외부 공지
- 2026-05-01: FE v2 시작
- 2026-06-30: 내부 이동 완료
- 2026-08-01: v1 제거

## Deprecation
- 헤더: `Deprecation: true, Sunset: Mon, 01 Aug 2026 00:00:00 GMT`
- Slack: #api-announcements
- OpenAPI: `deprecated: true`

## 검증
- [x] 계약 테스트
- [x] FE staging
- [ ] 모바일 TestFlight

## 롤백
v2 문제 시 FE flag로 v1 재호출

## References
```

## Exit Criteria
- [ ] breaking/non-breaking 확정
- [ ] 모든 소비자 + 오너
- [ ] 호환성 전략 문서화
- [ ] Expand → Migrate → Contract 타임라인
- [ ] 외부 공지 (있는 경우)
- [ ] 계약 테스트/OpenAPI diff 자동화

## 주의
- "호출하는 곳 없는 줄 알았는데 있었다" 사고 다발 — grep + 트래픽 로그 양쪽 확인
- 필드 의미 변경도 breaking (시멘틱)
- Kafka registry compatibility + 코드 양쪽
- 외부 deprecation 최소 3개월
- v1 트래픽 안 줄면 contract 연기 — 강제 제거 금지
