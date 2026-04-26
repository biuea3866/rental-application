# 배포 영향 분석 파이프라인

## 목적
릴리즈 후보(머지될 PR 묶음 또는 release branch)를 배포 직전 분석. 변경 / 리스크 / 롤백 / 모니터링 / 노트 일괄 준비.

## 담당 에이전트

| 역할 | 에이전트 |
|---|---|
| 오케스트레이션 | pipeline-runner |
| 아키텍처 영향 | be-tech-lead |
| 운영 리스크 | be-senior |
| 하네스 감사 | harness-auditor |

## 입력
- 릴리즈 범위 (release branch / PR 번호 리스트)
- 배포 대상 (prod / staging / canary)
- 배포 시점 (KST)
- 배포 책임자

## 단계

### 1. 변경 수집
- `git log <prev>..<release> --oneline`
- PR → 제목/설명/파일/Jira
- 카테고리: feature / bugfix / refactor / infra / docs

### 2. 영향 매핑
- 변경 서비스/모듈
- DB 스키마 (Flyway 목록)
- 토픽/Queue (Avro)
- 외부 API (버저닝 필요?)
- Feature flag 도입/제거

### 3. 배포 순서
- 마이그레이션 → 백엔드 → 프론트엔드
- 하위 호환(expand) 먼저, contract는 다음 릴리즈
- 토픽: Producer 전 Consumer 먼저
- 단계 간 health check 시간

### 4. 리스크 평가
- 고위험: 대량 마이그레이션, 결제/인증, 외부 API 계약, 파괴적 변경
- 중위험: 새 컨슈머/배치, 트래픽 10%+ 증가
- 저위험: 도메인 내부 리팩토링

각 PR에 risk = L/M/H.

### 5. 롤백 플랜
고위험 PR 별:
- Feature flag off (선호)
- 이전 태그 재배포
- 마이그레이션 역방향
- 롤백 불가 항목 별도 강조

### 6. 모니터링
- 변경 엔드포인트/배치 → 대시보드/알람 링크
- 새 메트릭
- 트래픽/에러율 기준선

### 7. 릴리즈 노트
- 사용자 공지 (기능/UX/정책)
- 내부 공지 (인프라/리팩토링)
- 채널: 이메일/웹/슬랙

## 산출물

```markdown
# Release <tag>

**Branch:** release/2026.04.26
**Target:** prod
**Owner:** @...

## Summary
<3줄>

## PR 목록
| # | 제목 | Cat | Risk | Rollback | Jira |
|---|---|---|---|---|---|

## 스키마
- Flyway: V2026_04_26_001
- Avro: `rental.order.placed` v3 (BACKWARD)

## 배포 순서
1. DB 마이그레이션 (14:00)
2. Consumer (14:05)
3. BE 카나리 10% (14:10) → 100% (14:25)
4. FE (14:30)

## 롤백
- PR #123: flag `order-v2` off
- PR #124: 이전 태그
- DB V001: `rollback/V001.sql`

## 모니터링
- /orders p99 < 500ms
- Kafka lag < 1000
- 에러율 < 0.5%
- 알람: Slack #alerts-release

## 릴리즈 노트 (고객)
> ...

## 릴리즈 노트 (내부)
- ...

## Sign-off
- [ ] Tech Lead
- [ ] QA
- [ ] PO
- [ ] SRE
```

## Exit Criteria
- [ ] 모든 PR risk + rollback
- [ ] 배포 순서 확정
- [ ] 고위험 롤백 검증
- [ ] 모니터링 확인
- [ ] 노트 초안
- [ ] 1명+ sign-off

## 주의
- 금요일/연휴 전 고위험 지양
- 마이그레이션과 코드 동일 PR 금지 (expand/contract)
- 카나리 10% 에러율 비교 필수
- 이전 릴리즈와 상호작용 명시
- 배포 후 30분 모니터링 당직 배정
