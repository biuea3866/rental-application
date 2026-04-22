# ADR-007: WebSocket 채팅 아키텍처

## Status
Accepted (2026-04-19) — Sprint 3에서 구현 완료, 회고적 문서화 (PRD-003 참조)

## Context

Sprint 3의 핵심 가설 H2 — "실시간 채팅이 대여 성사율을 높인다(채팅 시작 → 대여 신청 전환율 ≥ 50%)"를 검증하려면 대여자/등록자 간 실시간 메시지 교환이 필요하다.

### 후보 기술

| 후보 | 장점 | 단점 |
|------|------|------|
| HTTP Polling | 단순, 인프라 추가 없음 | 실시간성 부족, 불필요 트래픽, 서버 부하 |
| SSE (Server-Sent Events) | 단방향 푸시에 적합, HTTP/1.1 호환 | 양방향 아님, Client → Server는 별도 REST 필요 |
| WebSocket (raw) | 양방향 저지연, 표준 | 프레임 단위만 제공, 메시지 라우팅/구독 직접 구현 |
| **STOMP over WebSocket** | 양방향 + 구독 기반 라우팅 + Spring 1급 지원 | STOMP 프레임 규약 학습 필요 |

### 제약 조건
- Spring Boot 3.x + Kotlin 스택 재사용 필수 (별도 런타임 추가 회피)
- Sprint 3 범위 내 2주 안에 MVP 수준 구현
- 현재 단일 노드 배포 — 향후 수평 확장 시 브로커 교체 여지 확보
- 메시지 영속성: DB(`chat_message`)에 저장하여 히스토리 제공

## Decision

**STOMP over WebSocket + Spring `SimpleBroker`** 로 채팅을 구현한다.

### 엔드포인트 / 목적지 규약

```
[Client]
  ├─ connect    ws://.../ws           (SockJS 폴백 지원)
  ├─ subscribe  /topic/chat/{roomId}  (서버 → 클라이언트 브로드캐스트)
  └─ send       /app/chat/{roomId}    (클라이언트 → 서버)

[Server: ChatWebSocketController]
  @MessageMapping("/chat/{chatRoomId}")
  handleMessage() → SendChatMessageUseCase → SimpMessagingTemplate.convertAndSend("/topic/chat/{chatRoomId}")
```

### 핵심 설계 요소

1. **레이어 분리**
   - `ChatWebSocketController`: STOMP 프레임 수신만 담당, 비즈니스 로직 없음
   - `SendChatMessageUseCase`: 저장 + 이벤트 발행 오케스트레이션
   - `ChatDomainService`: 참여자 검증 + 메시지 생성 규칙
   - REST 엔드포인트(`ChatApiController`)는 채팅방 생성/목록/히스토리 조회 전용

2. **메시지 영속성**
   - `chat_message` 테이블에 모든 메시지 저장
   - STOMP 브로커는 메모리(SimpleBroker) — 연결이 없던 클라이언트는 재접속 시 DB에서 히스토리 로드

3. **인증**
   - 현재: STOMP CONNECT 시 `X-Member-Id` 헤더(ADR-005) 전파 → `senderId` 검증
   - `ChatDomainService.verifyParticipant()` 에서 "송신자가 해당 방 참여자인가" 도메인 규칙으로 검증

4. **SockJS 폴백**
   - 모바일 웹/회사 방화벽 등 WebSocket 차단 환경 대응
   - `registry.addEndpoint("/ws").withSockJS()`

## Consequences

### 장점
- Spring 1급 지원: `@MessageMapping`, `SimpMessagingTemplate` 로 라우팅/브로드캐스트를 선언적으로 처리
- 헥사고날 레이어 경계 유지: Controller → UseCase → DomainService 흐름이 REST와 동일
- 메시지 영속성으로 재접속 히스토리 제공
- SockJS 폴백으로 호환성 확보

### 단점
- SimpleBroker는 **단일 노드 전용** — 2대 이상 배포 시 서로 다른 노드에 붙은 구독자 간 브로드캐스트 불가
- 브로커가 메모리 기반이라 노드 재시작 시 세션 손실(히스토리는 DB에 있으므로 재접속으로 복구 가능)
- WebSocket 세션 수 증가 시 노드 당 커넥션 상한 관리 필요

### 수평 확장 시 마이그레이션 경로

| 단계 | 트리거 | 조치 |
|------|--------|------|
| 현재 | 단일 노드, MVP 검증 | SimpleBroker 유지 |
| 2단계 | 동시 접속 ≥ 수천 / 2대 이상 배포 필요 | SimpleBroker → **Redis Pub/Sub 기반 `StompBrokerRelay` (RabbitMQ/ActiveMQ)** 또는 Redis `enableStompBrokerRelay` 대체 구성 |
| 3단계 | 모바일 푸시까지 확장 | 오프라인 사용자용 푸시 채널 분리 (FCM/APNs) |

Controller/UseCase 레이어는 브로커 교체에 영향받지 않는다 — `SimpMessagingTemplate` 인터페이스가 동일하기 때문이다.

### 모니터링 포인트 (추후 작업)
- WebSocket 세션 수 (`SimpUserRegistry`)
- 메시지 처리 지연 (전송 → DB 저장 → 브로드캐스트 완료)
- 브로드캐스트 실패율

## References
- PRD-003 (Sprint 3 — 리뷰/채팅/관리자/정산)
- TDD-003
- [WebSocketConfig.kt](../../rental-api/src/main/kotlin/com/rental/commerce/presentation/api/config/WebSocketConfig.kt)
- [ChatWebSocketController.kt](../../rental-api/src/main/kotlin/com/rental/commerce/presentation/api/chat/ChatWebSocketController.kt)
