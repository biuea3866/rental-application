# ADR-003: 피처 플래그 시스템

- **Status**: Accepted
- **Date**: 2026-04-11

## Context

신규 기능 점진적 롤아웃, 플랜별 기능 제어, A/B 테스트, MSA 전환 시 트래픽 라우팅 등을 위해
피처 플래그 시스템이 필요하다.

## Decision

### 구현 방식
- 자체 구현: MySQL(마스터) + Redis(캐시) + 로컬 메모리 캐시(Caffeine) 3단계
- Admin API + 노션/Slack 연동

### 플래그 타입
- BOOLEAN: 단순 on/off
- PERCENTAGE: userId 기반 비율 롤아웃 (0~100%)
- TARGETING: 조건 기반 타겟팅 (유저 타입, 플랜 등)

### 3단계 캐시 전략
```
요청 → 로컬 메모리(Caffeine, TTL 30초) → Redis(TTL 5분) → MySQL
Admin 변경 시 → MySQL 업데이트 → Redis 삭제 → 이벤트 → 전체 인스턴스 로컬 캐시 무효화
```

### 평가 방식
- FeatureFlagService: 프로그래밍 방식 호출
- @FeatureFlag AOP: 선언적 사용 (domain 레이어에 위치)

### 패키지 구조
```
domain/featureflag/
├── FeatureFlag.kt              — Entity (AR)
├── FlagType.kt                 — Enum
├── FlagEvaluationContext.kt    — VO
├── FeatureFlagRepository.kt   — Interface
├── FeatureFlagService.kt      — 평가 서비스
├── FeatureFlagCacheReader.kt  — 캐시 읽기 interface
├── FeatureFlagAspect.kt       — AOP 어노테이션 + Aspect
├── FeatureDisabledException.kt
└── TargetRuleEvaluator.kt     — 타겟팅 규칙 평가

infrastructure/featureflag/
├── mysql/
│   ├── FeatureFlagJpaRepository.kt
│   └── FeatureFlagRepositoryImpl.kt
├── redis/
│   └── FeatureFlagRedisCache.kt
└── cache/
    └── FeatureFlagLocalCache.kt  — Caffeine (CacheReader 구현)

presentation/api/admin/
└── FeatureFlagAdminApiController.kt
```

### DDL
```sql
CREATE TABLE feature_flag (
    feature_flag_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '피처 플래그 ID',
    flag_key          VARCHAR(100) NOT NULL UNIQUE COMMENT '플래그 키',
    description       VARCHAR(500) NOT NULL COMMENT '설명',
    flag_type         VARCHAR(20)  NOT NULL COMMENT 'BOOLEAN, PERCENTAGE, TARGETING',
    enabled           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '전역 활성화',
    percentage        INT          NULL COMMENT '비율 롤아웃 (0~100)',
    target_rules      TEXT         NULL COMMENT '타겟팅 규칙 JSON',
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    INDEX idx_flag_key (flag_key)
) COMMENT '피처 플래그';
```

## Consequences

### 장점
- 3단계 캐시로 평가 성능 보장 (대부분 로컬 메모리에서 해결)
- AOP + Service 양방향 지원으로 유연한 사용
- 자체 구현으로 비용 없음, 도메인 맞춤 타겟팅 가능

### 단점/리스크
- 캐시 무효화 타이밍에 따라 최대 30초 지연 가능 (로컬 TTL)
- 타겟팅 규칙이 복잡해지면 자체 구현 유지보수 비용 증가
