# ADR-009: 분쟁(Dispute) 상태기계와 트랜잭션 경계

## Status
Draft (2026-04-22) — PRD-004 Sprint 4 사전 설계

## Context

PRD-004의 **분쟁(Dispute)** 기능은 대여 거래의 신뢰 회로를 닫는 마지막 조각이다. "거래가 실패했을 때 어떻게 해결하는가"를 명시하지 않으면 플랫폼 클레임이 CS 채널로 직접 쏟아진다.

### 제약 조건
- 동일 rental에 대해 활성 분쟁은 **최대 1개** (PRD-004 FR-1.4)
- 분쟁 결과가 **환불(Refund) + 정산(Settlement) 보정** 을 원자적으로 트리거해야 한다
- 분쟁 해결이 결제/정산 시스템과 **느슨하게 결합** 되어야 한다 (ADR-008 정산 이벤트 패턴과 일관)
- 관리자 개입이 필수인 상태와 자동 전이 가능한 상태를 분리한다

### 상태 후보
```
OPEN                  분쟁 생성 직후 (당사자 대기)
UNDER_REVIEW          관리자가 검토 착수
RESOLVED_REFUND       전체 환불로 종결
RESOLVED_PARTIAL      부분 환불로 종결
RESOLVED_REJECTED     분쟁 기각 (환불 없음)
CANCELLED             오픈자가 자발 철회
```

## Decision

**도메인 이벤트 + 명시 상태기계 + REQUIRES_NEW 리스너** 로 설계한다 — ADR-008(정산)과 동일한 모델 재사용.

### 1. 상태 전이 다이어그램

```
                          ┌──────────┐
     create(renter/lender)│          │
     ─────────────────────▶   OPEN   ├───── cancel() by opener ────▶ CANCELLED
                          │          │
                          └────┬─────┘
                               │ admin.startReview()
                               ▼
                        ┌──────────────┐
                        │ UNDER_REVIEW │
                        └──────┬───────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
  admin.resolveRefund()  admin.resolvePartial()  admin.resolveReject()
         │                     │                     │
         ▼                     ▼                     ▼
  RESOLVED_REFUND       RESOLVED_PARTIAL       RESOLVED_REJECTED
```

### 2. 상태 전이 규칙 (enum에 캡슐화)

```kotlin
enum class DisputeStatus {
    OPEN, UNDER_REVIEW,
    RESOLVED_REFUND, RESOLVED_PARTIAL, RESOLVED_REJECTED,
    CANCELLED;

    fun validateCanStartReview() { require(this == OPEN) }
    fun validateCanResolve()     { require(this == UNDER_REVIEW) }
    fun validateCanCancel()      { require(this == OPEN) } // UNDER_REVIEW 이후는 취소 불가
    fun isTerminal(): Boolean = this in TERMINAL

    companion object {
        private val TERMINAL = setOf(RESOLVED_REFUND, RESOLVED_PARTIAL, RESOLVED_REJECTED, CANCELLED)
    }
}
```

CLAUDE.md 규칙 — `enum에 상태 전이 규칙 캡슐화 (canTransitionTo, validateTransitionTo)` 와 일치.

### 3. DB 유니크 제약 (활성 분쟁 1개 보장)

```sql
-- MySQL: NULL 반복 허용을 이용한 부분 유니크
ALTER TABLE dispute
  ADD COLUMN active_rental_id BIGINT
    GENERATED ALWAYS AS (
      CASE WHEN status IN ('OPEN', 'UNDER_REVIEW') THEN rental_id ELSE NULL END
    ) STORED,
  ADD UNIQUE KEY uk_dispute_active_rental (active_rental_id);
```
- 활성 상태(OPEN/UNDER_REVIEW)일 때만 `rental_id` 값을 가지는 generated column에 UNIQUE
- 종결 상태로 전이되면 `NULL`이 되어 동일 rental에 새 분쟁 오픈 가능
- 도메인에서는 `DisputeDomainService.ensureNoActiveDispute(rentalId)` 로 1차 방어, DB UNIQUE로 2차 방어

### 4. 이벤트 흐름 — 분쟁 해결 → 환불 → 정산 보정

```
[Admin] POST /admin/disputes/{id}/resolve {type=PARTIAL, refundAmount=N}
    ↓
[ResolveDisputeUseCase] @Transactional
    ↓
    DisputeDomainService.resolve(dispute, type, refundAmount)
    ↓
    dispute.resolvePartial(refundAmount)  // 상태 전이 + 이벤트 등록
    eventPublisher.publish(DisputeResolvedEvent(disputeId, rentalId, resolutionType, refundAmount))
    ↓
    COMMIT
    ↓
[RefundProcessingListener]            — AFTER_COMMIT + REQUIRES_NEW
    RefundDomainService.processRefund(rentalId, refundAmount)  → PG 호출
      ↓
      RefundCompletedEvent
        ↓
[SettlementAdjustmentListener]        — AFTER_COMMIT + REQUIRES_NEW
    SettlementDomainService.applyRefundAdjustment(rentalId, refundAmount)
```

**핵심**: 각 단계는 **독립 트랜잭션**. 환불 PG 호출 실패가 분쟁 해결 결정을 롤백하지 않는다 — 관리자 UX 상 "결정은 저장됐는데 PG만 실패한 상태"는 재시도 가능해야 한다.

### 5. 실패 처리 정책 (Sprint 4 MVP)

| 지점 | 실패 동작 |
|------|-----------|
| 분쟁 상태 전이 (validation) | 도메인 예외 → 400, 상태 변경 없음 |
| 환불 PG 호출 실패 | `refund.status = FAILED` 저장, 분쟁은 `RESOLVED_*` 유지, **관리자 알림** |
| 정산 보정 실패 | 로그 + 알림, 수동 보정 경로 (ADR-008과 동일 철학) |

**자동 재시도는 Sprint 4 범위 밖** — Outbox 도입은 Sprint 5+ 별도 ADR.

### 6. 권한 모델

| 액션 | 허용 주체 |
|------|----------|
| create | renter OR lender (해당 rental 당사자만) |
| cancel (OPEN만) | 해당 분쟁의 opener |
| startReview / resolve* | ADMIN 권한 보유자만 |

`DisputeDomainService.verifyActor()` 에서 도메인 규칙으로 검증 — Controller/Gateway에 권한 로직을 새지 않게 한다.

## Consequences

### 장점
- ADR-008과 동일한 이벤트 모델을 재사용 → 팀 학습 비용 최소
- 상태기계가 enum에 캡슐화되어 누락된 전이가 컴파일 타임에 드러남
- DB UNIQUE로 중복 분쟁 차단을 이중 보장
- 환불/정산 보정이 분쟁 결정과 트랜잭션 분리 → PG 장애 시에도 결정은 보존

### 단점
- Generated column + UNIQUE 조합은 MySQL 8.0+ 전용 (현재 스택 OK)
- 환불 실패가 조용히 FAILED 상태로 남을 수 있음 → 관측 알람 필수
- 분쟁-환불-정산 3단계 비동기 체인은 관리자 UX에 "아직 반영 안 됨" 혼란 여지 → FE에서 `pendingActions` 뱃지 표기 필요

### 대안 기각 사유

| 대안 | 기각 사유 |
|------|----------|
| `status + rental_id` 복합 UNIQUE | 종결 상태가 여러 개 생기면 재오픈 불가해짐 |
| 애플리케이션 락만으로 중복 방지 | 다중 노드에서 레이스 조건 취약 |
| 환불을 분쟁 해결과 같은 트랜잭션에 묶기 | PG 장애가 분쟁 결정까지 롤백 — 관리자 반복 조작 필요 |
| 상태기계를 Service에 분산 | enum 캡슐화 규칙 위반, 누락된 전이 런타임에만 드러남 |

## References
- PRD-004 §4.1 (분쟁 처리)
- ADR-008 (정산 자동화 — 이벤트 패턴 원형)
- CLAUDE.md — enum 상태 전이 캡슐화 규칙, Transactional 위치 규칙
