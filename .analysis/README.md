# .analysis — 분석 파이프라인

각 서브디렉토리는 하나의 파이프라인이며, `PIPELINE.md`가 진입 문서이다.

## 파이프라인 목록 (9종)

| 디렉토리 | 용도 | 담당 에이전트 |
|---|---|---|
| `prd/` | PRD 분석 | prd-analyst |
| `project-analysis/` | 설계/TDD/티켓 분해 | project-analyst → ticket-splitter |
| `be-implementation/` | BE TDD 구현 + PR 생성 | be-implementer + be-senior |
| `pr-review/` | PR 리뷰 | pr-reviewer + harness-auditor (+ tech-lead/senior 병행) |
| `inquiry/` | 문의/버그 대응 | be-senior → be-implementer |
| `incident/` | 장애 대응 | be-tech-lead + be-senior |
| `release/` | 릴리즈 영향 | be-tech-lead + be-senior + harness-auditor |
| `refactoring/` | 리팩토링 계획 | be-tech-lead → be-implementer |
| `api-change/` | API 변경 | be-tech-lead + fe-lead |

## 사용법

1. 작업 성격에 맞는 파이프라인 선택
2. 해당 `PIPELINE.md`의 단계 수행
3. 산출물은 같은 디렉토리에 `YYYY-MM-DD-<slug>.md` 형식으로 누적

## 선택 가이드

- 새 기능 요청 → `prd` → `project-analysis` → `be-implementation`
- 고객 문의/버그 1건 → `inquiry`
- 장애 발생 → `incident`
- 머지 전 PR 확인 → `pr-review`
- 배포 전 묶음 검토 → `release`
- 구조 개선 → `refactoring`
- API 변경 → `api-change`

## pipeline-runner

여러 파이프라인 자동 오케스트레이션은 `pipeline-runner` 에이전트에게 위임. `/analyze-prd`, `/plan-project`, `/review-pr` 커맨드가 내부적으로 호출.
