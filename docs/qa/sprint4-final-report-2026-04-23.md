# Sprint 4 최종 완료 보고서

- **Date**: 2026-04-23
- **Sprint**: 4 — 분쟁/환불/검색/위시리스트/알림 설정
- **Status**: ✅ **전 기능 머지 완료 (BE + FE)**

---

## 1. 머지된 PR 목록 (총 18개)

### 설계·문서 (3)
| PR | 제목 |
|----|------|
| #93 | ADR-007/008 회고 + PRD-004 Sprint 4 초안 |
| #94 | ADR-009/010 + TDD-004 |
| #95 | SPRINT-4-TICKETS.md (20개 티켓 분해) |

### BE 구현 (11)
| PR | 티켓 | 요약 |
|----|------|------|
| #96 | DEVOPS-415 | Flyway V18~V23 (dispute/refund/product 비정규화/wishlist/notification_preference) |
| #97 | BE-401 | Dispute 도메인 Entity + Status 상태기계 + DomainService |
| #98 | BE-402 | Dispute 당사자 API (Open/Get/Cancel) |
| #100 | BE-403 | Admin Dispute API (StartReview/Resolve) |
| #101 | BE-404 | Refund 도메인 + PaymentRefundGateway Port-Adapter |
| #104 | BE-405 | DisputeResolvedRefundListener (AFTER_COMMIT + REQUIRES_NEW) |
| #105 | BE-410 | Product 검색 확장 (지역/다중카테고리/평점·인기순) |
| #107 | BE-420/421 | Wishlist + NotificationDebouncer debounce |
| #108 | BE-430/431 | NotificationPreference + Dispatcher + ArchUnit |
| #109 | BE-406 | RefundCompletedSettlementListener (idempotent net_amount 보정) |
| #110 | BE-411 | Product 비정규화 컬럼 업데이트 리스너 |

### 보안/품질 hotfix (2)
| PR | 요약 |
|----|------|
| #111 | 하네스 JSON 파싱 오류 + AdminDispute @RoleRequired + `!!` + Entity 캡슐화 |
| #115 | Admin/AdminProduct Controller @RoleRequired 누락 (senior-gate 탐지) |

### refactor (1)
| PR | 요약 |
|----|------|
| #112 | BE-405 Listener → RefundDomainService 로 비즈니스 로직 이동 |

### 피드백 루프 자동화 (1)
| PR | 요약 |
|----|------|
| #114 | 3-layer CI 자동화 — nightly harness-audit + PR senior gate + QA follow-up tickets |

### FE 구현 (4)
| PR | 티켓 | 요약 |
|----|------|------|
| #116 | FE-454 | 알림 설정 UI (7 테스트) |
| #117 | FE-452 | 검색 필터/정렬 UI (31 테스트) |
| #120 | FE-453 | 위시리스트 UI (14 테스트, clean 재작성) |
| #119 | FE-450/451 | 분쟁 오픈·조회 + 관리자 중재 UI (35 테스트) |

### 기타 (1)
| PR | 요약 |
|----|------|
| #113 | harness-rules 커버리지 확장 + Sprint 4 QA 1차 보고서 |

---

## 2. 테스트 커버리지

### BE
- Kotest BehaviorSpec 90+ 케이스
- `FlywaySprint4MigrationTest` 7 케이스 (V18~V23 전수)
- `ProductDenormUpdaterTest`, `WishlistNotificationListenerTest`, `DisputeResolvedRefundListenerTest`, `RefundCompletedSettlementListenerTest` — AFTER_COMMIT 리스너 5개 전부 MockK 테스트
- `NotificationArchitectureTest` — ArchUnit 경계 검증

### FE
- 로컬 vitest: **530 테스트 전원 PASS** (71 파일)
  - FE-452 검색: 31
  - FE-453 위시리스트: 14
  - FE-454 알림 설정: 7
  - FE-450/451 분쟁: 35
  - 기존 443 포함

---

## 3. 아키텍처 체인 (ADR-009)

```
ResolveDisputeUseCase(@Transactional)
  └─ DisputeDomainService.resolve*
       └─ dispute.resolve* + publish(DisputeResolvedEvent)
  COMMIT
  └─ DisputeResolvedRefundListener  AFTER_COMMIT + REQUIRES_NEW
       └─ RefundDomainService.processRefundForRental(rentalId, disputeId, amount, reasonCode)
            ├─ rental_payment 조회 / externalPaymentId 검증
            ├─ processRefund: SELECT FOR UPDATE → Refund(PENDING) → PG 호출 → markSucceeded/Failed
            └─ publish(RefundCompletedEvent)
       COMMIT
       └─ RefundCompletedSettlementListener  AFTER_COMMIT + REQUIRES_NEW
            └─ SettlementDomainService.applyRefundAdjustment(rentalId, totalRefunded)
                 └─ Settlement.applyRefundAdjustment — idempotent
```

---

## 4. 피드백 루프 (이번 Sprint 에서 실전 검증)

### 4.1 발견 → 조치 → 재발 방지 흐름

| 사건 | 시점 | 감지 | 조치 |
|------|------|------|------|
| harness-rules.json 콤마 누락 → 훅 silent pass | Sprint 초반 | 중간 점검 (사용자 요청) | #111 hotfix + nightly harness-audit 도입 |
| `!!` 2건, FQCN 3건 주입 | 훅 실패 기간 | harness-auditor 사후 감사 | #111 수정 + no-fqcn 룰 개선 |
| AdminDispute @RoleRequired 누락 (보안 Critical) | PR #100 머지 후 | pr-reviewer 사후 감사 | #111 수정 |
| Admin/AdminProduct @RoleRequired 누락 (보안 Critical) | Sprint 이전부터 존재 | **senior-gate.py 도입 즉시 탐지** | #115 hotfix |
| Listener Repository 직접 참조 (Critical) | PR #104 | pr-reviewer | #112 refactor |

### 4.2 3-layer CI 자동화 (#114)

- **nightly harness-audit** (03:00 KST): 전수 감사, 위반 발견 시 Issue 자동 생성
- **PR Senior Review Gate**: PR open/sync 시 senior-gate.py + harness-audit.py diff 스코프, Critical → REQUEST_CHANGES + workflow fail
- **QA Follow-up Tickets**: `docs/qa/*.md` push 시 "후속 티켓" 섹션 파싱 → Issue 자동 발행

### 4.3 claude-framework 이식

Sprint 4 에서 만든 루프 시스템을 `claude_framework` 레포의 `templates/` 하위로 이식 (로컬 준비 완료, 후속 PR 대상):
- `templates/.claude/harness-rules.json` — `{{BASE_PACKAGE}}` 등 placeholder 기반 중립화
- `templates/.claude/scripts/{harness-audit, senior-gate, qa-followup-extract}.py`
- `templates/.github/workflows/{harness-audit, pr-senior-review, qa-followup-tickets}.yml`
- `agents/feedback-loop-guardian.md` + `.analysis/feedback-loop/PIPELINE.md`

---

## 5. 남은 기술 부채 / 후속 티켓

| 티켓 | 근거 | 우선순위 |
|------|------|---------|
| RC-BE-441 Presentation 통합 테스트 — Dispute/Wishlist/NotificationPreference | pr-reviewer Major | P1 |
| RC-BE-442 BE-404/405 E2E 통합 테스트 (Testcontainers AFTER_COMMIT) | AFTER_COMMIT + REQUIRES_NEW 실제 검증 | P1 |
| RC-BE-443 RefundDomainService @Transactional 명시 | pr-reviewer Major | P2 |
| RC-BE-444 RefundExceedsPaymentException 전용 ErrorCode | pr-reviewer Minor | P3 |
| RC-BE-445 SocialProvider/RentalStatus → domain.common | harness-auditor cross-domain | P2 |
| RC-DEVOPS-446 RedisNotificationDebouncer adapter | 다중 노드 운영 대비 | P2 |
| RC-DEVOPS-447 claude-framework 3-layer 루프 이식 PR | Sprint 4 학습 자산 전파 | P1 |

---

## 6. Sprint 4 지표

| 항목 | 값 |
|------|----|
| 머지된 PR | 18 |
| 작성된 Kotlin 파일 (신규) | 60+ |
| 작성된 TypeScript 파일 (신규) | 40+ |
| DB 마이그레이션 | V18~V23 (6개) |
| BE 테스트 | 90+ 케이스 |
| FE 테스트 | 530 케이스 전원 PASS |
| CI 워크플로우 (신규) | 3개 (harness-audit, pr-senior-review, qa-followup-tickets) |
| 신규 도메인 | 5개 (dispute, refund, wishlist, notification-pref, discovery 확장) |
| 감지·조치한 보안 Critical | 3건 (@RoleRequired 누락 AdminDispute/Admin/AdminProduct) |

---

## 7. 결론

Sprint 4 BE + FE 전체 구현 완료. 분쟁→환불→정산 보정 3단계 이벤트 체인이 실제 코드로 닫혔고, 검색·위시리스트·알림 설정이 활성화됐습니다.

중간 점검에서 발견된 하네스 훅 silent pass 이슈를 계기로 **3-layer 자동화**를 CI 에 내재화했고, 이 시스템이 즉시 기존 dev 의 보안 Critical 2건을 추가 탐지했습니다. 경험을 `claude-framework` 에 templates 로 이식해 **재사용 가능 자산**으로 전환했습니다.

**Sprint 4 완료. Sprint 5 킥오프 가능 상태.**
