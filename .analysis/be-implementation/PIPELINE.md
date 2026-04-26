# BE 구현 파이프라인

## 목적
project-analysis 산출물(설계 + 티켓)을 받아 코드까지 내려오는 구간. TDD(Red→Green→Refactor) 강제, 하네스 룰 위반 없이 PR까지.

## 담당 에이전트 & 스킬

| 역할 | 에이전트 | 스킬 |
|---|---|---|
| 오케스트레이션 | pipeline-runner | — |
| 구현 | be-implementer | kotlin-spring-impl, tdd-loop |
| 리뷰 게이트 | be-senior, be-tech-lead | pr-review-checklist |
| 하네스 감사 | harness-auditor | harness-audit |

호출:
```
/tdd-implement <ticket-id>
/parallel-tickets <epic-id>
```

## 입력
- 티켓 ID 또는 경로
- 대응 Acceptance Criteria
- 테스트 케이스 ID
- 영향 범위 (어떤 모듈)

## 단계

### 1. 준비
- 티켓에서 AC, 의존성, TC 추출
- worktree checkout: `feature/<ticket-id>`
- 관련 코드 탐색 (Grep + Glob)

### 2. Red — 테스트 먼저
- Kotest BehaviorSpec (Given/When/Then)
- 통합테스트 우선 (Testcontainers MySQL/Redis/Kafka)
- 단위테스트는 도메인 로직만
- `./gradlew :module:test --tests "<FQN>" -i` 실행 → 실패 확인
- 통과해 버리면 테스트가 잘못된 것

### 3. Green — 최소 구현
- 레이어: Controller → Facade → UseCase → DomainService → Repository
- @Transactional은 UseCase/DomainService만
- 엔티티에 비즈니스 로직 캡슐화 (Rich Domain), Service 얇게
- 금지 (하네스 차단):
  - @Query → QueryDSL CustomRepository + Impl
  - LocalDateTime → ZonedDateTime
  - ConsumerRecord<String,String> → DTO 매핑
  - FK / DB-Enum / JSON / BOOLEAN 컬럼
  - FQCN import

### 4. Refactor
- 중복/네이밍/SRP 교정
- 매 리팩토링마다 테스트 재실행
- YAGNI: 지금 필요 없는 추상화 금지

### 5. 통합 보강
- 예외 경로
- 동시성/idempotency
- 외부 API: Port-Adapter + Wiremock/Testcontainers

### 6. 자체 감사
- `/audit-harness`로 변경 파일 검사
- `be-senior` 병행 스폰 → 운영 리스크 반영

### 7. 커밋 & PR
- 브랜치: `feature/<jira-key>-<slug>` (base=dev)
- 커밋: Conventional Commits + Jira 키
- PR 본문에 AC 체크박스 + 테스트 로그
- Jira "In Review"

## 산출물

`.analysis/be-implementation/YYYY-MM-DD-<ticket-id>.md`

```markdown
# <ticket-id> — <title>

**Branch:** feature/RC-1234-foo

## AC 체크
- [x] AC-01
- [ ] AC-02 — 미구현 사유

## 테스트
- 통과: 42 / 실패: 0 / 커버리지: 95.3%

## 변경
- 신규: FooUseCase, FooDomainService
- 수정: BarFacade

## 운영
- [x] 롤백 플랜
- [x] 모니터링
- [x] 마이그레이션

## PR
<link>
```

## Exit Criteria
- [ ] 모든 AC 통과 (또는 사유 기록)
- [ ] 통합테스트, 커버리지 95%+
- [ ] /audit-harness critical 0
- [ ] be-senior 리뷰 반영
- [ ] PR + Jira 업데이트

## 병렬 실행
여러 티켓 동시 구현:
- 티켓당 worktree 1 + be-implementer 1
- 팀장(Opus) orchestrate, 팀원(Sonnet) 개별
- 의존성 그래프 토폴로지 정렬
- 티켓당 PR 1개 원칙

## 주의
- main/dev 직접 푸시 금지
- Red 단계 누락 = 실패
- 도메인 경계 변경은 ADR 먼저
- 외부 API: Port + Gateway + Factory 필수
