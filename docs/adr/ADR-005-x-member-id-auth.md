# ADR-005: X-Member-Id 헤더 기반 인증 전환

## Status
Accepted (2026-04-12)

## Context

현재 rental-api는 `JwtAuthenticationFilter`에서 JWT를 직접 파싱하여 `SecurityContextHolder`에 인증 정보를 설정하고, 각 Controller에서 `SecurityContextHolder.getContext().authentication.principal`을 통해 userId를 추출하는 구조입니다.

### 현재 구조 (AS-IS)
```
[Client] → Bearer Token → [JwtAuthenticationFilter] → JWT 파싱/검증 → SecurityContextHolder → [Controller] → extractUserId()
```

### 문제점
1. **관심사 미분리**: 내부 서비스(rental-api)가 JWT 토큰 파싱과 비즈니스 로직을 모두 담당
2. **중복 검증**: BFF/API Gateway가 추가되면 JWT 검증이 이중으로 수행됨
3. **컨트롤러 반복 코드**: 모든 Controller마다 `extractUserId()` private 메서드가 중복
4. **테스트 복잡성**: Controller 테스트에서 `SecurityContextHolder`를 직접 조작해야 함

## Decision

`X-Member-Id` 커스텀 헤더 기반 인증으로 전환합니다.

### 변경 구조 (TO-BE)
```
[Client] → Bearer Token → [JwtAuthenticationFilter] → JWT 파싱 → X-Member-Id, X-Member-Role 헤더 설정
                                                                          ↓
                           [MemberIdArgumentResolver] ← @AuthenticatedMember ← [Controller]
```

### 핵심 변경사항

1. **JwtAuthenticationFilter**: JWT 파싱 후 `SecurityContextHolder` 설정은 유지하되, `request.setAttribute("X-Member-Id", userId)` 추가
2. **@AuthenticatedMember 어노테이션**: Controller 파라미터에 선언하면 자동으로 userId 주입
3. **MemberIdArgumentResolver**: `HandlerMethodArgumentResolver` 구현, `X-Member-Id` 속성/헤더에서 userId 추출
4. **Controller 리팩토링**: `extractUserId()` 제거 → `@AuthenticatedMember userId: Long` 파라미터 사용

### 향후 확장 (BFF/Gateway 도입 시)
```
[Client] → Bearer Token → [API Gateway/BFF] → JWT 파싱 → X-Member-Id 헤더 → [rental-api]
                                                                                    ↓
                           JwtAuthenticationFilter는 X-Member-Id 헤더만 읽음 (JWT 파싱 제거)
```

## Consequences

### 장점
- Controller 코드 간결화 (`extractUserId()` 중복 제거)
- 테스트 용이성 (헤더만 설정하면 인증 시뮬레이션 가능)
- BFF/Gateway 도입 시 내부 서비스 변경 최소화
- 관심사 분리 (인증 → Filter, 비즈니스 → Controller)

### 단점
- X-Member-Id 헤더 위조 방어 필요 (내부 네트워크에서만 신뢰)
- Filter-Controller 간 계약이 헤더/속성 기반으로 암묵적

### 위조 방어 전략
- 현재 단계: JwtAuthenticationFilter가 JWT 검증 후에만 X-Member-Id 설정 (외부 헤더 무시)
- Gateway 도입 시: Gateway만 X-Member-Id 설정 가능, 내부 서비스는 헤더 신뢰

## References
- ADR-002: Layer Architecture
- rental-api/src/main/kotlin/.../common/JwtAuthenticationFilter.kt
- rental-api/src/main/kotlin/.../common/SecurityConfig.kt
