# Harness Scripts — 피드백 루프 자동화

Sprint 4 중간 점검에서 발견된 문제 — 훅이 JSON 파싱 오류로 silent pass 된 동안 룰 위반이 누적된 사례 — 를 재발 방지하기 위한 CI 기반 피드백 루프.

## 목록

### `harness-audit.py`

harness-rules.json 의 forbidden_patterns 를 프로젝트 전체에 적용하는 감사 도구.

- **용도**: 훅 미적용 기간 / 신규 룰 추가 시 누적 위반 전수 발견
- **주석 제외**: `//`, `--`, `/* */`, `*` 라인 주석 / 블록 주석 필터링으로 false positive 최소화
- **실행**:
  ```bash
  python3 .claude/scripts/harness-audit.py
  # exit 0: 위반 없음
  # exit 1: warning 만 있음
  # exit 2: error 있음
  ```
- **연동**: `.github/workflows/harness-audit.yml` 에서 nightly (18:00 UTC = 03:00 KST) 실행 → 위반 감지 시 `harness-audit` 라벨로 Issue 자동 생성/업데이트

### `senior-gate.py`

pr-reviewer 에이전트가 발견한 Critical 유형을 CI 에서 저비용으로 선제 차단.

- **현재 커버 규칙**:
  1. Admin Controller `@RoleRequired("ADMIN")` 누락 (PR #100 재발 방지)
  2. Controller 에 `HttpServletRequest` 직접 주입 (PR #98)
  3. Listener 에 `Repository` 직접 주입 (PR #104 → #112)
  4. `@Service` UseCase 의 `@Transactional` 누락
- **실행**:
  ```bash
  find ... | python3 .claude/scripts/senior-gate.py "$(git diff --name-only origin/dev...HEAD | grep '\.kt$')"
  ```
- **연동**: `.github/workflows/pr-senior-review.yml` 에서 PR 오픈/업데이트 시 자동 — Critical 발견 시 `REQUEST_CHANGES` 리뷰 코멘트 + workflow fail

### `qa-followup-extract.py`

QA 보고서의 "## 후속 티켓 제안" 섹션을 파싱해 GitHub Issue 자동 생성용 JSON 출력.

- **지원 헤딩**: `## N. 후속 티켓`, `## 추가 후속 티켓`, `### 후속 티켓`, `## Follow-up Tickets`
- **테이블 포맷**:
  ```
  | 티켓 초안 | 근거 |
  |-----------|------|
  | RC-BE-441 설명 — 디테일 | pr-reviewer 지적 |
  ```
- **연동**: `.github/workflows/qa-followup-tickets.yml` 에서 `docs/qa/*.md` push 감지 시 자동 실행 → `qa-followup` 라벨로 Issue 생성 (중복 제목은 skip)

## 전체 피드백 루프 구조

```
┌──────────────────────────────────────────────────────────┐
│  Dev Push                                                │
├──────────────────────────────────────────────────────────┤
│  1. PreToolUse 훅 (로컬 Write/Edit) — harness-check.py   │
│     → 즉시 BLOCKED (exit 2)                              │
├──────────────────────────────────────────────────────────┤
│  2. PR open → pr-senior-review.yml                       │
│     → senior-gate.py + harness-audit.py                  │
│     → Critical → REQUEST_CHANGES + workflow fail        │
├──────────────────────────────────────────────────────────┤
│  3. Merge → qa-followup-tickets.yml                      │
│     (docs/qa/*.md 변경 시)                               │
│     → qa-followup-extract.py → Issue 자동 생성           │
├──────────────────────────────────────────────────────────┤
│  4. Nightly 03:00 KST → harness-audit.yml                │
│     → 전수 감사 + harness-audit 라벨 Issue               │
└──────────────────────────────────────────────────────────┘
```

## 관련 문서
- `docs/qa/sprint4-qa-report-2026-04-23.md` §2 (훅 silent pass 복구 전말)
- `.claude/harness-rules.json` (룰 정의)
- `.claude/harness-check.py` (로컬 훅)
