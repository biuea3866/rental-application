# 문의/버그 대응 파이프라인

## 목적
CS/운영/사용자 문의 또는 버그 1건을 재현 → 원인 분석 → 수정/회신까지. 재발 방지 포함.

## 담당 에이전트

| 역할 | 에이전트 |
|---|---|
| 오케스트레이션 | pipeline-runner |
| 재현/원인 | be-senior |
| 수정 구현 | be-implementer (skill: tdd-loop, kotlin-spring-impl) |
| 회신 초안 | pipeline-runner |

## 입력
- 문의 ID (Zendesk/Notion)
- 재현 정보 (workspace id, 시각, 페이로드)
- 심각도: P0(중단) / P1(장애) / P2(UX) / P3(질문)
- SLA

P0는 `incident/PIPELINE.md` 사용.

## 단계

### 1. 분류
- 버그 / 사용법 / 정책 / 기능 요청
- 버그가 아니면 회신 초안만 후 종료

### 2. 재현
- 로컬/스테이징 동일 시나리오
- 불가 시: 로그/APM/DB에서 워크스페이스·시각 트레이스
- 재현 git sha + 데이터 상태 기록

### 3. 원인 분석
- 코드 경로 추적 (grep → 호출 시퀀스)
- `git log -p <file>` 블레임
- 데이터 원인 vs 코드 원인 구분
- 영향 범위: "다른 워크스페이스도?" 쿼리

### 4. 수정안 분류
- 코드 수정 → `be-implementation/PIPELINE.md`로
- 데이터 수정 → 1회성 SQL + DBA 승인 + 결과 로그
- 설정/flag 수정 → 변경 + 롤백 플랜
- 가이드 부재 → 문서 작성

### 5. 재발 방지
- 동일 이슈 방어 테스트 1개 이상
- 알람 추가 여부
- 하네스 룰 등록 제안

### 6. 회신 초안
- 고객 언어 (기술 용어 최소화)
- 원인/조치/ETA/우회
- 내부 아키텍처 노출 금지

## 산출물

`.analysis/inquiry/YYYY-MM-DD-<inquiry-id>.md`

```markdown
# <inquiry-id> — <description>

**Severity:** P1
**Reporter:** workspace=1234
**SLA:** 2026-04-26 EOD

## 재현
- 환경: prod
- 절차: 1) ... 2) ...
- sha: abc123

## 원인
<설명>
- 코드: `FooService.kt:42`
- 커밋: `abc123 (2026-03-10)`

## 영향 범위
- 영향 워크스페이스: 12개
- 데이터 손상: 없음

## 조치
- [x] 코드: PR #1234 (RC-5678)
- [x] 데이터: `2026-04-26-fix-foo.sql` 12 row 정정
- [ ] 회신 초안: CS 승인 대기

## 재발 방지
- 테스트: `FooServiceTest.should_not_*`
- 알람: Foo 실패율 > 1%
- 룰: `no-null-workspace-id`

## 회신 초안
> 안녕하세요, ...

## References
```

## Exit Criteria
- [ ] 재현 가능/사유 기록
- [ ] 원인 특정
- [ ] 영향 범위 파악
- [ ] 조치 완료 또는 티켓 발행
- [ ] 재발 방지책 1개+
- [ ] 고객 회신 초안

## 주의
- 재현 안 되면 로그/APM 증거 또는 추가 정보 요청
- 데이터 정정은 DBA 리뷰 필수
- P0 징후(다수 워크스페이스/결제/인증/데이터 손상) → incident로 에스컬레이션
- 회신에 내부 코드/테이블/sha 포함 금지
