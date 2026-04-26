# 리팩토링 파이프라인

## 목적
기능 변경 없는 구조 개선을 점진 실행. "작게 쪼개서, 테스트 보호막 아래, 동작 동일성 증명". 빅뱅 금지.

## 담당 에이전트

| 역할 | 에이전트 |
|---|---|
| 오케스트레이션 | pipeline-runner |
| 아키텍처 | be-tech-lead |
| 운영 | be-senior |
| 구현 | be-implementer (skill: tdd-loop, kotlin-spring-impl) |

## 입력
- 동기 ("왜 지금?" — 새 기능 블로커, 반복 버그, 성능)
- 범위 (패키지/서비스 경계/전역 패턴)
- 제약 (배포 윈도우, 팀 이관, freeze)
- 성공 기준 (측정 가능: 테스트 시간, 빌드 시간, 버그율)

## 단계

### 1. 동기 검증 — Do Nothing 비교
- 안 하면 생기는 비용 구체적 기술
- 리팩토링 비용 vs 유지 비용
- 의사결정: Go / No-go / Defer
- No-go/Defer면 종료

### 2. 현상 파악
- 측정값: LOC, 순환복잡도, 커버리지, 의존성
- 핫스팟: `git log --since=1y --name-only`
- 테스트 보호막 없으면 **테스트 추가가 선행 티켓**

### 3. 목표 상태
- After 스냅샷 (구조/디렉토리/책임)
- Mermaid (Component/Sequence) 전후 대비
- 도메인 경계 이동이면 ADR

### 4. 작업 분해
각 step:
- 독립 머지 가능 (behavior-preserving)
- 테스트 통과 유지
- 롤백 가능
- 1 PR 크기

패턴:
- **Strangler Fig**: 신규+기존 공존 → 호출자 이동 → 기존 제거
- **Expand-Contract**: 추가 → 이동 → 제거
- **Branch by Abstraction**: 인터페이스 → 구현 교체 → 단순화

### 5. 보호장치
- 커버리지 목표 (90%+ before)
- Benchmark 기준선
- Feature flag
- 모니터링 추가

### 6. 일정 분배
- 티켓 분해 (`ticket-splitter`)
- 의존성 그래프
- 다른 팀 영향 공지

### 7. 실행 루프
step별:
1. 테스트 보강 (필요 시)
2. 변경 + 테스트 통과
3. 성능/동작 동일성
4. PR 머지
5. 프로덕션 모니터링 24h+
6. 다음 step

## 산출물

```markdown
# Refactoring: <topic>

**Owner:** @...
**Duration:** 3 sprints

## Motivation
<현재/안 하면/Do Nothing 기각 사유>

## Current State
- 범위: `module/foo/**`
- LOC: 12,400
- Cyclomatic: avg 8.2
- Coverage: 72%
- 핫스팟: `FooService.kt` (1년 34회)

## Target State
<Mermaid>
- 책임: A → B, C
- 의존성: domain ← application ← infra

## 전략: Strangler
1. `FooService2` (behavior-preserving)
2. BarFacade 이동
3. BazFacade 이동
4. FooService 제거
5. FooService2 → FooService 리네임

## 티켓
| # | Step | Risk | 예상 | 전제 |
|---|---|---|---|---|

## 보호장치
- 커버리지 95%
- Benchmark p99 ±5%
- Flag `new-foo-impl`

## 모니터링
- 비교 메트릭 기존 vs 신규
- 에러 차이 > 0.1% 자동 롤백

## Success Criteria
- [ ] 테스트 시간 8분 → 3분
- [ ] 신규 기능 리드타임 40%↓

## Rollback
각 step PR revert 가능
```

## Exit Criteria
- [ ] Do Nothing 비교로 정당화
- [ ] 각 step 독립 머지/롤백
- [ ] 테스트 보호막 확보
- [ ] 성능 회귀 측정 기준
- [ ] 티켓 발행
- [ ] Success 1+ 측정

## 주의
- "그냥 정리하고 싶어서"는 이유 아님
- 테스트 없으면 테스트 추가 선행
- behavior-preserving 원칙
- scope creep — 발견 문제는 별도 티켓
- PR 1000 LOC 넘기 시작하면 분해 재검토
