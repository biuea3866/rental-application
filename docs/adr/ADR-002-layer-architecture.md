# ADR-002: 레이어 아키텍처 및 컨벤션

- **Status**: Accepted
- **Date**: 2026-04-11

## Context

대여/구독 플랫폼의 백엔드 레이어 구조와 각 레이어별 책임, 컨벤션을 정의해야 한다.
모놀리스에서 시작하되 MSA 전환을 고려한 도메인 경계가 명확한 구조가 필요하다.

## Decision

### 전체 패키지 구조

```
com.rental.commerce/
├── presentation/                  # 외부 진입점 (얇게)
│   ├── api/                       # REST API
│   │   ├── rental/
│   │   │   ├── RentalApiController.kt
│   │   │   ├── CreateRentalRequest.kt
│   │   │   └── CancelRentalRequest.kt
│   │   ├── product/
│   │   │   ├── ProductApiController.kt
│   │   │   └── RegisterProductRequest.kt
│   │   ├── lender/
│   │   ├── renter/
│   │   ├── payment/
│   │   ├── review/
│   │   ├── notification/
│   │   ├── report/
│   │   └── common/
│   │       ├── ErrorResponse.kt
│   │       └── ErrorCode.kt
│   ├── worker/                    # Kafka Consumer
│   │   ├── rental/
│   │   │   └── RentalEventWorker.kt
│   │   ├── payment/
│   │   │   └── PaymentEventWorker.kt
│   │   └── notification/
│   │       └── NotificationEventWorker.kt
│   ├── batch/                     # Spring Batch Job
│   │   ├── rental/
│   │   │   └── OverdueRentalJob.kt
│   │   └── subscription/
│   │       └── SubscriptionRenewalJob.kt
│   └── socket/                    # WebSocket (STOMP + SockJS)
│       └── chat/
│           └── ChatSocketHandler.kt
│
├── application/                   # UseCase 단위
│   ├── rental/
│   │   ├── CreateRentalUseCase.kt
│   │   ├── ConfirmRentalUseCase.kt
│   │   ├── CancelRentalUseCase.kt
│   │   ├── ReturnRentalUseCase.kt
│   │   ├── CreateRentalCommand.kt
│   │   └── RentalResponse.kt
│   ├── product/
│   │   ├── RegisterProductUseCase.kt
│   │   ├── RegisterProductCommand.kt
│   │   └── ProductResponse.kt
│   ├── lender/
│   ├── renter/
│   ├── payment/
│   ├── deposit/
│   ├── subscription/
│   ├── review/
│   ├── notification/
│   ├── report/
│   └── chat/
│
├── domain/                        # 순수 비즈니스 로직
│   ├── rental/
│   │   ├── Rental.kt             # AR — 엔티티 (Rich Domain Model)
│   │   ├── RentalStatus.kt       # Enum — 상태 전이 캡슐화
│   │   ├── RentalPeriod.kt       # VO
│   │   ├── RentalRepository.kt   # Interface
│   │   ├── RentalService.kt      # Domain Service
│   │   └── event/
│   │       ├── RentalConfirmedEvent.kt
│   │       ├── RentalCompletedEvent.kt
│   │       └── DomainEvent.kt
│   ├── product/
│   │   ├── Product.kt
│   │   ├── ProductImage.kt
│   │   ├── PricePolicy.kt        # VO
│   │   ├── Stock.kt
│   │   ├── ProductRepository.kt
│   │   └── ProductService.kt
│   ├── lender/
│   │   ├── Lender.kt
│   │   ├── LenderType.kt
│   │   ├── VerificationStatus.kt
│   │   ├── LenderRepository.kt
│   │   └── LenderService.kt
│   ├── renter/
│   │   ├── Renter.kt
│   │   ├── TrustGrade.kt
│   │   ├── RenterRepository.kt
│   │   └── RenterService.kt
│   ├── subscription/
│   │   ├── Subscription.kt
│   │   ├── SubscriptionPlan.kt
│   │   ├── SubscriptionStatus.kt
│   │   ├── SubscriptionRepository.kt
│   │   └── SubscriptionService.kt
│   ├── payment/
│   │   ├── Payment.kt
│   │   ├── PaymentMethod.kt      # VO
│   │   ├── Refund.kt
│   │   ├── PaymentRepository.kt
│   │   ├── PaymentService.kt
│   │   └── PaymentGateway.kt     # Port (interface)
│   ├── deposit/
│   │   ├── Deposit.kt
│   │   ├── DepositStatus.kt
│   │   ├── DepositRepository.kt
│   │   └── DepositService.kt
│   ├── review/
│   │   ├── Review.kt
│   │   ├── ReviewRepository.kt
│   │   └── ReviewService.kt
│   ├── notification/
│   │   ├── Notification.kt
│   │   ├── NotificationChannel.kt
│   │   ├── NotificationRepository.kt
│   │   ├── NotificationService.kt
│   │   └── NotificationGateway.kt # Port
│   ├── chat/
│   │   ├── ChatRoom.kt
│   │   ├── ChatMessage.kt
│   │   ├── ChatRoomRepository.kt
│   │   └── ChatService.kt
│   ├── report/
│   │   ├── Report.kt
│   │   ├── ReportStatus.kt
│   │   ├── ReportRepository.kt
│   │   └── ReportService.kt
│   └── common/
│       ├── DomainEvent.kt
│       └── BaseEntity.kt
│
└── infrastructure/                # 기술 구현체
    ├── rental/
    │   ├── mysql/
    │   │   ├── RentalJpaRepository.kt
    │   │   └── RentalRepositoryImpl.kt
    │   ├── kafka/
    │   │   └── RentalKafkaProducer.kt
    │   └── event/
    │       └── RentalGradeListener.kt
    ├── product/
    │   ├── mysql/
    │   │   ├── ProductJpaRepository.kt
    │   │   └── ProductRepositoryImpl.kt
    │   └── redis/
    │       └── ProductCacheClient.kt
    ├── lender/
    │   ├── mysql/
    │   │   ├── LenderJpaRepository.kt
    │   │   └── LenderRepositoryImpl.kt
    │   └── gateway/
    │       └── BusinessVerificationGatewayImpl.kt
    ├── renter/
    │   └── mysql/
    ├── subscription/
    │   └── mysql/
    ├── payment/
    │   ├── mysql/
    │   ├── kafka/
    │   └── gateway/
    │       └── TossPaymentGatewayImpl.kt
    ├── deposit/
    │   └── mysql/
    ├── review/
    │   └── mysql/
    ├── notification/
    │   ├── mysql/
    │   ├── kafka/
    │   └── gateway/
    │       ├── KakaoAlimtalkGatewayImpl.kt
    │       ├── SmsGatewayImpl.kt
    │       └── PushGatewayImpl.kt
    ├── chat/
    │   └── mongo/
    ├── report/
    │   └── mysql/
    └── common/
        ├── event/
        │   ├── FailedEvent.kt
        │   ├── FailedEventRepository.kt
        │   └── EventRetryHandler.kt
        └── redis/
            └── DistributedLockClient.kt
```

### 레이어별 책임 요약

| Layer | 책임 | 의존 방향 |
|-------|------|----------|
| Presentation | 라우팅, 인증, Request→Command 변환, UseCase 호출 | → Application |
| Application | UseCase 단위 오케스트레이션, 트랜잭션, Response 반환 | → Domain |
| Domain | Entity 비즈니스 로직, Repository interface, Gateway interface | 의존 없음 |
| Infrastructure | DB, Kafka, Redis, 외부 API 구현체 | → Domain (interface 구현) |

### 컨벤션 요약

**네이밍**
- Controller: `~ApiController.kt`
- UseCase: `~UseCase.kt` (행위 1개 = 1 클래스)
- Entity: 도메인명 그대로 (`Rental.kt`, `Product.kt`)
- Repository: `~Repository.kt` (Domain interface), `~JpaRepository.kt` + `~RepositoryImpl.kt` (Infra)
- Gateway: `~Gateway.kt` (Domain interface), `~GatewayImpl.kt` (Infra)
- Worker: `~EventWorker.kt`

**DTO 흐름**
```
Request (presentation) → Command (application) → Entity (domain)
Entity (domain) → Response (application) → 그대로 반환
```

**트랜잭션**
- UseCase에 @Transactional (기본)
- 이벤트 발행은 @TransactionalEventListener(AFTER_COMMIT)

**이벤트 구독**
- 같은 도메인: 동기 (@TransactionalEventListener)
- 다른 도메인: 비동기 (@Async + @Retryable)
- 실패: RDB 저장 + 알림 + Admin API 재처리

**상태 전이**
- Enum 내부에 canTransitTo() 캡슐화

## Consequences

### 장점
- 각 레이어 책임 명확, 도메인별 패키지로 MSA 전환 시 분리 용이
- UseCase 단위 설계로 행위가 명시적, 테스트 쉬움
- Rich Domain Model로 비즈니스 로직이 Entity에 응집

### 단점/리스크
- UseCase 클래스 수가 많아질 수 있음 → 도메인별 패키지로 관리
- 동일 도메인 내 이벤트 리스너가 많아질 수 있음 → 리스너 1개에 여러 핸들러 메서드로 관리
