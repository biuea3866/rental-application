# ADR-010: 검색 엔진 선택 — Sprint 4는 MySQL 유지

## Status
Accepted (2026-04-22) — PRD-004 Sprint 4 대상. Sprint 6에 재평가.

## Context

PRD-004 §4.3은 카테고리/가격 범위/위치(시·구)/가용 기간 필터 + 5가지 정렬을 요구한다. 응답 시간 SLA는 **p95 < 500ms**. 자연어 키워드 검색은 단순 LIKE 수준.

### 후보

| 엔진 | 장점 | 단점 | 운영 비용 |
|------|------|------|-----------|
| **MySQL (현재 스택)** | 인프라 추가 없음, 트랜잭션 일관성, QueryDSL 호환 | 한국어 풀텍스트 부족, 정렬/필터 카디널리티 낮으면 인덱스 튜닝으로 충분 | 0 |
| PostgreSQL + `pg_trgm`/GIN | 한국어 유사도 검색 | DB 교체 필요, 이전 스프린트 결정 뒤집음 | 높음 |
| Elasticsearch/OpenSearch | 전용 풀텍스트/Faceted/한국어 형태소(Nori) | 이중 쓰기, 인덱싱 지연, 재색인 복잡성 | 중~높음 |
| Meilisearch / Typesense | Dev UX 우수, 한국어 토크나이저 약함 | 국내 레퍼런스 부족, 단일 장애점 | 중 |

### 트래픽 가정 (Sprint 4 기준)
- 상품 row: ~10만 이하
- 검색 QPS 평균: ~20, 피크: ~100
- 필터 조합 중 **고카디널리티 키워드 기반 full-text**는 후순위 (P2)
- 키워드보다 **카테고리/가격/위치/기간 필터 기반 탐색이 지배적**

이 규모에서 Elasticsearch 도입은 **조기 최적화**로 판단된다. 이중 쓰기 파이프라인/재색인 훈련/운영 알람을 Sprint 4 안에 안정화하는 비용 대비, MySQL 인덱스 튜닝으로 p95 < 500ms는 달성 가능.

## Decision

**Sprint 4에서는 MySQL + QueryDSL 동적 쿼리를 유지한다.** Elasticsearch/OpenSearch 도입은 아래 **트리거 조건** 중 하나라도 만족될 때 Sprint 6+에서 ADR 재작성 후 도입한다.

### 인덱스 전략 (MVP)

```sql
-- 카테고리 + 상태 + 가격 범위 + 지역은 단일 커버링 인덱스로 공략
CREATE INDEX idx_product_search
  ON product (status, category_id, region_code, price);

-- 기간 조회는 rental/availability 테이블에서 별도 커버 (join)
CREATE INDEX idx_availability_product_period
  ON product_availability (product_id, available_from, available_to);
```

### 정렬 처리
| 정렬 | 구현 |
|------|------|
| 최신순 | `ORDER BY product.created_at DESC` (PK 역순) |
| 가격 낮은순/높은순 | 위 커버링 인덱스 활용 |
| 평점 높은순 | `product.rating_avg` **비정규화 컬럼** — 리뷰 저장 시 업데이트 |
| 인기순(대여 횟수) | `product.rental_count` **비정규화 컬럼** — RETURNED 이벤트에서 증가 |

비정규화 컬럼 업데이트는 ADR-008과 동일한 `TransactionalEventListener(AFTER_COMMIT)` 패턴으로 처리 — 리뷰 생성/대여 반납 이벤트를 구독.

### Elasticsearch 전환 트리거 (Sprint 4 이후 관측)

| 트리거 | 근거 |
|--------|------|
| 검색 p95 > 500ms 지속 2주 | SLA 미달 |
| 상품 row ≥ 50만 | MySQL B+Tree 랜덤 I/O 비용 급상승 |
| 한국어 풀텍스트 검색 전환율이 주요 지표로 승격 | Nori 형태소 없이 LIKE만으로 대응 불가 |
| Faceted navigation (다중 집계 카운트)가 P0로 승격 | MySQL GROUP BY 비용 폭증 |

### 전환 경로 (참고 — 실제 의사결정은 별도 ADR에서)
```
1. CDC (Debezium) 또는 Outbox로 product/review/rental 이벤트 스트림 확보
2. Indexer 워커: MySQL → ES 초기 bulk 색인 + 증분 동기화
3. 검색 쿼리만 ES로 스위치, 나머지 CRUD는 MySQL 유지
4. 인기순/평점순 컬럼은 ES score 함수로 대체 가능
```

## Consequences

### 장점
- 인프라 추가 없음 — Sprint 4 범위를 넘지 않음
- 테스트 용이: Testcontainers MySQL 한 개로 통합 테스트 완결
- 기존 QueryDSL 학습 곡선 그대로 재사용
- 비정규화 컬럼이 이후 ES 도입 시 score 필드로 그대로 승격 가능

### 단점 / 리스크
- 한국어 키워드 검색 품질이 LIKE 수준 → 키워드 중심 UX는 Sprint 4에서 의도적으로 약함
- 상품 row 확장 속도가 예상보다 빠르면 p95 미달 가능 → **k6 부하 테스트로 월 1회 회귀 측정 필수** (PRD-004 AC 참조)
- 비정규화 컬럼 누락 시 정렬 품질 저하 → 이벤트 리스너 실패 알람 필수

### 기각된 대안 요약
- **PostgreSQL 전환**: 이전 스택 결정(ADR-002 계열)을 뒤집는 것은 Sprint 4 스코프 밖
- **ES 선제 도입**: 이중 쓰기/재색인 운영 비용이 검증되지 않은 가설(H3) 대비 과투자

## References
- PRD-004 §4.3, §5, §6.3
- ADR-008 (AFTER_COMMIT 리스너 패턴 — 비정규화 컬럼 갱신에 재사용)
- CLAUDE.md — QueryDSL 규칙 (`@Query` 금지)
