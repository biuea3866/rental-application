# ADR-FE-001: FE 기술 스택 및 아키텍처

- **Status**: Accepted
- **Date**: 2026-04-11
- **Related**: ADR-001, ADR-002, TDD-FE-001

## Context

대여/구독 C2C/B2C 플랫폼의 프론트엔드 기술 스택과 아키텍처를 결정해야 한다.
모바일+웹 단일 코드베이스로 PWA 기반 서비스를 제공하며, BE(Kotlin/Spring Boot)와 병렬 개발한다.

핵심 요구사항:
1. 모바일/웹 반응형 UI (등록자/대여자/관리자 역할별 화면)
2. 오프라인 지원(PWA) — 상품 목록 캐싱, 홈 화면 추가
3. 소셜 로그인(카카오/네이버) + JWT Token Rotation 인증
4. Presigned URL 이미지 업로드 (FE -> MinIO 직접)
5. 다단계 상품 등록 폼 + Draft 임시저장

## Decision

### 프레임워크: Next.js 15 (App Router) + TypeScript

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| Next.js 15 (App Router) | RSC, Streaming SSR, Parallel Routes, 대형 생태계 | 러닝 커브(App Router) | **채택** |
| Remix | Nested Routes, 폼 처리 우수 | 생태계 작음, PWA 지원 약함 | 미채택 |
| Nuxt.js | Vue 기반, SSR 내장 | Vue 경험 부족, 채용 풀 한정 | 미채택 |
| SPA (Vite+React) | 단순, 빠른 빌드 | SSR/SEO 불가, 초기 로딩 느림 | 미채택 |

- App Router: React Server Components, Streaming SSR, Parallel Routes 지원
- TypeScript strict mode로 타입 안정성 확보
- Route Groups: `(auth)`, `(lender)`, `(renter)`, `(admin)`, `(common)`으로 역할별 라우트 분리

### PWA (Progressive Web App)

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| PWA (next-pwa) | 웹 코드 재사용, 앱스토어 불필요, 오프라인 | iOS Push 제한 | **채택** |
| React Native | 네이티브 성능 | 별도 코드베이스, 빌드/배포 복잡 | 미채택 |
| Flutter Web | 단일 코드 | SEO 불가, 웹 성능 열위 | 미채택 |

- next-pwa로 Service Worker 자동 생성 + manifest.json 설정
- 오프라인 캐싱: 상품 목록, 이미지, 정적 페이지
- 홈 화면 추가, Push 알림 수신 (Web Push API)
- iOS Safari 제한 → 웹소켓 실시간 알림으로 보완

### 스타일링: Tailwind CSS + shadcn/ui

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| Tailwind + shadcn/ui | 유틸리티, 접근성, 커스터마이징 자유 | shadcn 자동 업데이트 불가 | **채택** |
| MUI | 완성도 높은 컴포넌트 | 번들 크기 큼, 커스터마이징 어려움 | 미채택 |
| Ant Design | 관리자 UI 우수 | 디자인 자유도 낮음 | 미채택 |

- Tailwind CSS v4: 유틸리티 퍼스트, 빌드 시 사용 클래스만 포함
- shadcn/ui: Radix Primitives 기반 접근성 보장, 소스 코드 복사 방식으로 커스터마이징 자유
- 디자인 토큰: CSS 변수로 색상/간격/타이포 관리

### 상태관리: Zustand (클라이언트) + TanStack Query (서버)

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| Zustand + TanStack Query | 경량, 서버/클라이언트 분리 명확 | 두 라이브러리 학습 | **채택** |
| Redux Toolkit + RTK Query | 공식 도구, 예측 가능 | 보일러플레이트 과다 | 미채택 |
| Jotai | 원자적 상태, 간결 | 서버 상태 관리 부족 | 미채택 |
| SWR | 단순 캐싱 | Mutation 약함, Optimistic 불편 | 미채택 |

- Zustand: 경량(2KB), 보일러플레이트 최소, persist 미들웨어로 Draft 폼 상태 로컬 유지
- TanStack Query v5: 서버 상태 캐싱, 자동 재요청, Optimistic Update, Infinite Query(상품 목록)
- 상태 분리 원칙: UI 상태 → Zustand, 서버 데이터 → TanStack Query

### API 통신: fetch + OpenAPI codegen

- BE OpenAPI spec에서 TypeScript 타입+fetch 클라이언트 자동 생성 (openapi-typescript-codegen)
- BE/FE 계약 동기화 보장, 타입 불일치 방지
- 네이티브 fetch 사용 — Next.js App Router의 캐시/revalidation 전략 활용
- API 클라이언트 래퍼: JWT 자동 첨부, Token Refresh 인터셉터, 에러 핸들링 공통화

### 테스트: Vitest + Testing Library + Playwright (E2E)

| 레이어 | 도구 | 범위 |
|--------|------|------|
| 단위 테스트 | Vitest | 유틸, 훅, 스토어 |
| 컴포넌트 테스트 | Testing Library | UI 컴포넌트, 폼 동작 |
| E2E 테스트 | Playwright | 전체 화면 흐름, 크로스 브라우저 |

- Vitest: Vite 기반 빠른 단위 테스트, Jest 호환 API
- React Testing Library: 사용자 관점 컴포넌트 테스트, 접근성 쿼리
- Playwright: 크로스 브라우저(Chromium/Firefox/WebKit), Visual Regression

### 프로젝트 구조

```
rental-web/
├── public/                    # 정적 파일 (manifest.json, icons)
├── src/
│   ├── app/                  # Next.js App Router
│   │   ├── (auth)/           # 인증 관련 라우트 그룹
│   │   │   ├── login/
│   │   │   ├── signup/
│   │   │   └── layout.tsx
│   │   ├── (lender)/         # 등록자 라우트 그룹
│   │   │   ├── dashboard/
│   │   │   ├── products/
│   │   │   └── layout.tsx
│   │   ├── (renter)/         # 대여자 라우트 그룹
│   │   │   ├── home/
│   │   │   ├── search/
│   │   │   └── layout.tsx
│   │   ├── (admin)/          # 관리자 라우트 그룹
│   │   │   ├── inspection/
│   │   │   └── layout.tsx
│   │   ├── (common)/         # 공통 라우트
│   │   │   ├── mypage/
│   │   │   └── notifications/
│   │   ├── layout.tsx        # 루트 레이아웃
│   │   └── page.tsx          # 스플래시/랜딩
│   ├── components/           # 공유 UI 컴포넌트
│   │   ├── ui/               # shadcn/ui 컴포넌트
│   │   ├── layout/           # Header, Footer, Navigation
│   │   ├── product/          # 상품 관련 컴포넌트
│   │   └── auth/             # 인증 관련 컴포넌트
│   ├── hooks/                # 커스텀 훅
│   ├── lib/                  # 유틸리티, API 클라이언트
│   │   ├── api/              # OpenAPI codegen 출력
│   │   ├── auth/             # JWT 관리, 소셜 로그인
│   │   └── utils/            # 공통 유틸
│   ├── stores/               # Zustand 스토어
│   ├── types/                # TypeScript 타입 정의
│   └── styles/               # 글로벌 스타일
├── tests/                    # 테스트
│   ├── unit/                 # Vitest 단위 테스트
│   ├── integration/          # 통합 테스트
│   └── e2e/                  # Playwright E2E
├── next.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── vitest.config.ts
├── playwright.config.ts
└── package.json
```

## Consequences

### 장점
- 모바일+웹 단일 코드베이스로 개발/유지보수 비용 절감
- SSR + PWA 조합으로 SEO + 오프라인 + 네이티브급 UX 동시 달성
- OpenAPI codegen으로 BE/FE 계약 자동 동기화, 타입 불일치 방지
- Zustand persist로 Draft 폼 상태 유실 방지 (브라우저 새로고침/이탈 대응)
- Route Groups로 역할별 레이아웃/미들웨어 분리 명확

### 단점/리스크
- PWA는 iOS Safari에서 Push 알림/백그라운드 동기화 제한 → 웹소켓 실시간 알림으로 보완
- shadcn/ui 소스 복사 방식이라 자동 업데이트 불가 → 프로젝트 내 버전 관리 필요
- 카메라/갤러리 접근 시 네이티브 대비 제한 → HTML5 input[type=file] + capture 속성 활용
- App Router 러닝 커브 → 초기 설정 시 컨벤션 문서화 + 코드 리뷰로 대응

## Document History

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2026-04-11 | 1.0 | 초안 작성 — 기술 스택 결정 |
