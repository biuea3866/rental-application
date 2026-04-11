# Rental Commerce 티켓 작성 가이드

> 모든 노션 티켓은 이 가이드의 구조를 따라야 한다.

## 티켓 기본 구조 (전 직군 공통)

### 1. 개요
- **목적**: 이 티켓이 해결하는 문제/구현하는 기능
- **배경**: 이 작업이 필요하게 된 컨텍스트, 관련 ADR/TDD 링크

### 2. 작업 내용
- **작업 목록**: 체크리스트 형태
- **다이어그램**: Mermaid (sequenceDiagram, flowchart, stateDiagram-v2 등)
  - 반드시 1개 이상의 다이어그램 포함

### 3. 테스트 케이스
- **정상 케이스 (Happy Path)**: to_do 체크리스트
- **예외 케이스 (Edge Case)**: to_do 체크리스트
- **경계값 케이스 (Boundary)**: to_do 체크리스트 (해당 시)

---

## BE 티켓 추가 항목

### 데이터 모델
- **테이블 DDL**: `CREATE TABLE` 전체 SQL
  - FK/ENUM/JSON/BOOLEAN 금지, TINYINT(1), DATETIME(6), COMMENT 필수
- **Enum 정의**: 테이블 (값 | 설명 | 전이 가능 상태)
- **ERD 관계**: 테이블 간 관계 (FK 없이 애플리케이션 레벨 참조)

### API 스펙
- **메서드 + URL** (예: `POST /api/v1/products`)
- **Request Body / Response**: JSON 코드블록
- **Validation Rules**: 필드 | 규칙 | 에러 코드
- **Error Responses**: 상태코드 | 에러코드 | 설명

### Kafka 스펙 (해당 시)
- **Producer**: 토픽, eventType, 페이로드
- **Consumer**: 토픽, 처리 로직, 에러 처리
- 토픽 네이밍: `event.rental.{domain}` (RentalTopics 상수)

### 시퀀스 다이어그램
- Mermaid `sequenceDiagram` (정상 + 에러)

### 테스트 시나리오
- **단위 테스트**: Kotest BehaviorSpec (Given/When/Then)
- **통합 테스트**: TestContainers (MySQL, Redis, Kafka)

---

## FE 티켓 추가 항목

### 페이지/컴포넌트 구조
- 페이지 라우팅, 컴포넌트 트리, 상태 관리

### API 연동 스펙
- 호출 BE API, Request/Response 타입, 에러 핸들링

### UI/UX 요구사항
- 반응형, 인터랙션, 접근성

---

## DevOps 티켓 추가 항목

### 인프라 구성
- Docker Compose / CI/CD / 모니터링

### 구성 파일 스펙

---

## 공통 규칙

- **코드 컨벤션**: ZonedDateTime, QueryDSL, 엔티티 캡슐화, Facade 패턴
- **DB 규칙**: FK/ENUM/JSON 금지, TINYINT(1) boolean, DATETIME(6), COMMENT 필수
- **Kafka**: `event.rental.{domain}` 토픽, DTO 직접 매핑
- **시간**: ZonedDateTime (LocalDateTime 금지)
- **테스트**: Kotest BehaviorSpec, Given/When/Then, TestContainers
