# ADR-008: 정산 자동화 전략

## Status
Accepted (2026-04-19) — Sprint 3에서 구현 완료, 회고적 문서화 (PRD-003 참조)

## Context

Sprint 3의 핵심 가설 H3 — "투명한 정산 내역이 등록자 이탈률을 낮춘다"를 검증하려면, 대여 반납(RETURNED) 시점에 **자동으로** 정산이 생성되어야 한다. 누락 시 플랫폼 신뢰도 치명타.

### 요구사항
1. 대여 상태가 `RETURNED`로 전이되는 순간 정산 1건이 생성된다.
2. 수수료율(10%) 기준으로 `commission`, `netAmount`를 계산해 `settlement` 테이블에 저장한다.
3. 동일 `rentalId`에 대한 중복 정산은 발생하지 않는다.
4. 정산 로직 실패가 **대여 반납 자체를 롤백하면 안 된다** (별도 보상 가능).
5. 향후 정산 완료(`PENDING → COMPLETED`)는 은행 연동/사람 처리 후 별도 트리거.

### 후보 트리거 방식

| 후보 | 장점 | 단점 |
|------|------|------|
| Rental UseCase 내부에서 직접 호출 | 단순, 트랜잭션 1개 | 책임 혼재, 정산 실패 시 반납도 롤백됨, 결합도↑ |
| Kafka Producer → 별도 Consumer | 서비스 분리 가능, 수평 확장 | 인프라 의존성↑, Sprint 3 범위 초과, 1노드 MVP에 과설계 |
| **Spring `@TransactionalEventListener(AFTER_COMMIT)`** | 반납 트랜잭션과 정산 트랜잭션 분리, 인프라 추가 없음, 도메인 이벤트 표준 | Producer/Consumer 같은 JVM 필요(현재 전제와 부합) |

## Decision

**도메인 이벤트 + `@TransactionalEventListener(AFTER_COMMIT)` + `Propagation.REQUIRES_NEW`** 로 정산을 자동화한다.

### 이벤트 흐름

```
[Client] → POST /rentals/{id}/return
    ↓
[ReturnRentalUseCase] ─ @Transactional
    ↓
    RentalDomainService.return(rental)
    ↓
    rental.markReturned()
    eventPublisher.publish(RentalStatusChangedEvent(rentalId, lenderId, rentalAmount, toStatus=RETURNED))
    ↓
    COMMIT (대여 반납 트랜잭션 종료)
    ↓
[RentalReturnedSettlementListener]
  @TransactionalEventListener(AFTER_COMMIT)
  @Transactional(propagation = REQUIRES_NEW)
    ↓
    runCatching { SettlementDomainService.createSettlement(...) }
      .onSuccess { log }
      .onFailure { log.error }  // 반납 롤백 없음
```

### 핵심 설계 요소

1. **AFTER_COMMIT 보장**
   - 대여 반납이 실제로 커밋된 후에만 리스너가 실행 → "대여는 됐는데 정산만 생성"되는 것은 가능하지만 "정산은 됐는데 대여가 롤백"은 불가능
   - `BEFORE_COMMIT`을 쓰면 대여 커밋 직전 정산 실패로 반납까지 롤백될 수 있어 의도와 어긋남

2. **REQUIRES_NEW 트랜잭션 경계**
   - 리스너는 **새 트랜잭션**으로 정산을 저장
   - 정산 실패가 반납 트랜잭션에 영향 못 줌 (이미 커밋됨)
   - `CLAUDE.md` 규칙 "@Transactional은 UseCase/DomainService에서만"에 대한 **명시적 예외** — 이벤트 리스너는 별도 트랜잭션 경계의 소유자이므로 Infrastructure에 위치해도 허용

3. **중복 정산 방지**
   - `settlement.rental_id UNIQUE` DB 제약 (Flyway V13)
   - `SettlementDomainService.createSettlement()` 내부에서 기존 정산 존재 시 early return
   - 이중 방어: 이벤트가 중복 발행돼도 UNIQUE 위반으로 실패, `runCatching`이 삼킴

4. **실패 처리 — runCatching + 로그**
   - 자동 재시도 없음 (MVP 단계 의도적 단순화)
   - 실패 시 `ERROR` 로그 기록 → 관측 후 수동 보정
   - 향후: **정산 실패 이벤트 → Outbox → 재시도 워커** 또는 **관리자 대시보드 수동 재정산 버튼**으로 확장

5. **Observer 패턴 명시**
   - harness 규칙 `design_patterns.observer` 준수
   - 이벤트 발행자(`RentalDomainService`)는 수신자(`SettlementListener`)를 모름 → 결합 최소화
   - 향후 정산 외 다른 후행 처리(예: 리뷰 작성 알림 트리거, 배지 발급)도 동일 이벤트를 구독하기만 하면 됨

### 수수료 계산 정책
- `commission = amount × 0.10` (소수점 2자리 반올림 HALF_UP)
- `netAmount = amount − commission`
- 수수료율은 현재 리스너 상수 `COMMISSION_RATE = 0.10` → **향후 정책 변경 시 DB/설정 기반 주입 필요** (기술 부채로 기록)

## Consequences

### 장점
- 반납과 정산의 **트랜잭션 독립성** 확보
- 인프라 추가 없이 도메인 이벤트로 느슨한 결합 달성
- UNIQUE 제약 + 도메인 검증 이중 방어로 중복 정산 차단
- 향후 Kafka/Outbox 도입 시 이벤트 명세가 그대로 호환

### 단점 / 리스크
- **실패 자동 재시도 없음** — 정산 실패는 수동 보정이 필요
- **수수료율 하드코딩** — 정책 변경 시 배포 필요
- 이벤트 리스너 예외가 삼켜지면 모니터링 없을 경우 조용한 손실 가능 → 로그 알람 필수
- AFTER_COMMIT 이벤트는 JVM 내부에서만 동작 → 다중 노드 환경에서 대여 반납이 노드 A, 정산 처리도 노드 A에서만 실행됨(동일 JVM 전제)

### 마이그레이션 경로

| 단계 | 트리거 | 조치 |
|------|--------|------|
| 현재 | 단일 JVM, MVP | `@TransactionalEventListener` 유지 |
| 2단계 | 정산 실패율 > 1% 또는 다중 노드 배포 | `RentalStatusChangedEvent` → **Outbox 테이블 + Kafka Producer** 전환, 별도 워커가 Consumer |
| 3단계 | 정책 변경 빈도↑ | `commission_policy` 테이블 or 설정 스토어(Runtime Config)에서 수수료율 조회 |

### 관측 지표
- `settlement` 생성 지연(반납 커밋 → 정산 row 생성 시간차)
- 정산 생성 실패 카운트 (로그 기반 메트릭)
- `rental_id` UNIQUE 위반 카운트 (중복 이벤트 징후)

## References
- PRD-003 (Sprint 3)
- TDD-003
- [Settlement.kt](../../rental-domain/src/main/kotlin/com/rental/commerce/domain/settlement/Settlement.kt)
- [RentalReturnedSettlementListener.kt](../../rental-infrastructure/src/main/kotlin/com/rental/commerce/infrastructure/settlement/RentalReturnedSettlementListener.kt)
- [Flyway V13 — settlement DDL](../../rental-infrastructure/src/main/resources/db/migration)
