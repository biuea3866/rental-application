# PR 리뷰 파이프라인

## 목적
PR 1건을 하네스 룰 + 아키텍처 + 테스트 + 보안 + 성능 관점에서 검수하고 verdict(approve / request-changes / comment)를 남긴다.

## 담당 에이전트 & 스킬

| 역할 | 에이전트 | 스킬 |
|---|---|---|
| 오케스트레이션 | `pipeline-runner` | — |
| 룰 감사 | `harness-auditor` | `harness-audit` |
| 기계적 체크리스트 | `pr-reviewer` | `pr-review-checklist` |
| 운영 리스크 관점 | `be-senior` | — |
| 아키텍처 관점 | `be-tech-lead` / `fe-lead` | — |

`be-senior`/`be-tech-lead` 병행 스폰. 셋이 상충하면 사용자에게 에스컬레이션.

호출:
```
/review-pr <PR URL 또는 번호>
```

## 입력
- PR URL/번호 (필수)
- 리뷰 범위(옵션): full / diff-only / tests-only
- base branch (기본: dev)

## 단계

### 1. PR 메타 수집
- `gh pr view <n> --json title,body,baseRefName,headRefName,labels,files,commits`
- `gh pr diff <n>`
- 카테고리 분류 (BE/FE/infra/docs/test)

### 2. 하네스 룰 감사 (harness-auditor)
- `harness-rules.json`의 `forbidden_patterns`를 변경 파일에 적용
- `file:line:rule_id` 매핑
- severity=error는 verdict=request-changes 고정

### 3. 체크리스트 (pr-reviewer + pr-review-checklist)
- 레이어 경계 (Controller→Facade→UseCase→Domain→Repo)
- 트랜잭션 위치(@Transactional은 UseCase/DomainService만)
- N+1 / 페치조인 누락
- 변경된 Service에 Testcontainers 통합테스트 포함 여부
- 입력 검증, 시크릿, 권한
- 컨슈머는 Facade 경유, DTO 직접 매핑

### 4. 아키텍처/운영 (병행 스폰)
- be-tech-lead: 서비스 경계, 데이터 소유권, 마이그레이션
- be-senior: 장애 시나리오, 롤백, idempotency
- fe-lead (FE PR): 재사용성, 디자인 시스템, 상태 관리

### 5. 결과 집계
- critical / major / minor / nit 분류
- critical ≥1 → request-changes
- major만 1-2 → comment
- minor/nit만 → approve
- `gh pr review --body <summary> --<verdict>`

### 6. Jira/문서 동기화
- PR 제목의 Jira 키 → "In Review"
- 머지 후 자동 "Done"은 GitHub Actions 처리

## 산출물

`.analysis/pr-review/YYYY-MM-DD-pr-<number>.md`

```markdown
# PR #<n> — <title>

**Verdict:** request-changes
**Author:** @...
**Base:** dev
**Jira:** RC-1234

## Critical
- `path/File.kt:42` — @Query 사용 (rule: no-jpa-query). QueryDSL로 재작성.

## Major
- 트랜잭션이 Repository에서 시작 — UseCase로 이동.

## Minor / Nit
- 네이밍 정리

## 운영 리스크 (be-senior)
## 아키텍처 (be-tech-lead)
## References
```

## Exit Criteria
- [ ] 모든 변경 파일 하네스 감사
- [ ] critical 발견 시 verdict=request-changes
- [ ] 병행 리뷰어 코멘트
- [ ] PR 코멘트 게시
- [ ] Jira 상태 업데이트

## 주의
- main/dev 직접 푸시 커밋 섞임 → critical
- 테스트 없는 Service 신규 → critical
- PR 본문 비면 작성 요청 후 verdict 보류
- FE PR은 디자인 시스템 토큰 하드코딩 확인
