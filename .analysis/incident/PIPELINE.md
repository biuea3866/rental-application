# 장애 대응 파이프라인

## 목적
P0/P1 장애: 완화 → 복구 → RCA → 재발 방지. 다수 사용자 영향 건.

**완화 속도 > 원인 규명**.

## 담당 에이전트

| 역할 | 에이전트 |
|---|---|
| 인시던트 커맨더 | pipeline-runner |
| 아키텍처 판단 | be-tech-lead |
| 운영/장애 경험 | be-senior |
| 수정 구현 | be-implementer (skill: tdd-loop, kotlin-spring-impl) |

병행 스폰: tech-lead + senior 동시 가설 제시.

## 입력
- 감지 채널 (모니터링/CS/제보)
- 심각도: P0(전면 중단) / P1(핵심 기능)
- 상태: ongoing / mitigated / resolved
- 시작 시각 (ISO-8601)

## 단계

### 1. 선언
- Slack #incident
- 인시던트 시트 생성
- 커맨더/서기/엔지니어 지정
- 통신 창구 (고객 공지)

### 2. 현상 파악
- 영향 범위 (워크스페이스/지역/기능)
- 시작 시점 (배포·설정 변경 상관?)
- 대조군 (정상 경로)
- 증거 영구 저장 (로그/APM/쿼리/Kafka lag)

### 3. 완화 (Mitigate First)
순서:
1. Feature flag off
2. 이전 배포 rollback
3. 트래픽 제어 (rate limit)
4. 임시 핫픽스

메트릭으로 완화 성공 확인.

### 4. 복구 확인
- 정상 동작 (회기 + 사용자 플로우)
- 지연 데이터/Kafka 백로그 처리
- 고객 공지 업데이트

### 5. RCA
- 5 Whys
- 단일 엔지니어 실수로 귀결 금지 — 시스템적 원인
- 타임라인 (감지→선언→완화→복구) ISO-8601 분 단위
- 기여 요인 vs 근본 원인 분리

### 6. Action Items
- Owner + Due + Jira 필수
- 카테고리: Detection / Prevention / Mitigation / Recovery
- 후속 리뷰에서 추적

### 7. Postmortem (Blameless)
- 개인 비난 금지 — 프로세스/시스템 관점
- 파일: `.analysis/incident/YYYY-MM-DD-<slug>.md`
- 이해관계자 리뷰 후 공유

## 산출물

```markdown
# INC-YYYY-MM-DD-<slug>

**Severity:** P0
**Status:** resolved
**Commander:** @...
**Duration:** 48분

## TL;DR
<3-5문장>

## 영향
- 워크스페이스: 1,200
- 기능: 결제 API
- 에러율 peak: 87%
- 데이터 손상: 없음
- 고객 접수: 34건

## 타임라인 (KST)
- 14:02 — 알람
- 14:05 — 선언
- 14:10 — 가설 #1 (DB 풀 고갈)
- 14:35 — 롤백 시작
- 14:48 — 정상화

## 감지
- 경로: Grafana
- TTD: 3분

## 완화
- 순서: flag → 롤백
- TTM: 30분

## Root Cause
<5 Whys>

## Action Items
| # | Owner | Due | Category | Jira |
|---|---|---|---|---|

## 교훈
- 잘한 점 / 개선할 점

## References
```

## Exit Criteria
- [ ] 서비스 복구 (정상 메트릭 30분+)
- [ ] 고객 공지
- [ ] 타임라인 완성
- [ ] RCA 작성, 근본 원인 1+
- [ ] Action Items Owner/Due/Jira
- [ ] Blameless 리뷰

## 주의
- 완화 우선 — 복구 중 원인 완전 이해 시도 금지
- 개인 비난 금지
- 추측은 "가설" 표기
- 데이터 복구 DBA 승인 필수
- 반복 인시던트(3+) → 별도 프로젝트
