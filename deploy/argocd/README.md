# ArgoCD 배포 가이드

## 아키텍처

```
GitHub Actions (CI/CD)          ArgoCD (GitOps)              Kubernetes
┌─────────────────┐       ┌──────────────────┐       ┌──────────────┐
│ 1. 빌드 + 테스트  │       │ 3. Git 변경 감지   │       │ 4. 배포 적용   │
│ 2. GHCR 이미지 푸시│  ──▶  │    Kustomize 렌더 │  ──▶  │    Pod 교체    │
│    태그 커밋      │       │    diff 확인      │       │    롤링 업데이트 │
└─────────────────┘       └──────────────────┘       └──────────────┘
```

### 배포 흐름

**Dev (자동)**
1. feature 브랜치 → dev PR 머지
2. CD Dev 파이프라인: 빌드 → GHCR 푸시 → `overlays/dev/kustomization.yml` 태그 갱신 커밋
3. ArgoCD가 dev 브랜치 변경 감지 → 자동 sync → 클러스터 배포

**Prod (수동 승인)**
1. dev → main PR 머지 (QA 통과 후)
2. CD Prod 파이프라인: 빌드 → GHCR 푸시 → `overlays/prod/kustomization.yml` 태그 갱신 커밋
3. ArgoCD UI에서 diff 확인 → 수동 `argocd app sync rental-commerce-prod`

## 초기 설정

### 1. ArgoCD 설치 (클러스터에 한 번)
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

### 2. ArgoCD CLI 설치
```bash
brew install argocd
```

### 3. 초기 비밀번호 확인 + 로그인
```bash
argocd admin initial-password -n argocd
argocd login localhost:8080
```

### 4. AppProject + Application 등록
```bash
kubectl apply -f deploy/argocd/project.yml
kubectl apply -f deploy/argocd/application-dev.yml
kubectl apply -f deploy/argocd/application-prod.yml
```

### 5. GHCR 이미지 접근 (프라이빗 레포인 경우)
```bash
kubectl create secret docker-registry ghcr-credentials \
  --docker-server=ghcr.io \
  --docker-username=biuea3866 \
  --docker-password=<GITHUB_TOKEN> \
  -n rental-commerce

kubectl create secret docker-registry ghcr-credentials \
  --docker-server=ghcr.io \
  --docker-username=biuea3866 \
  --docker-password=<GITHUB_TOKEN> \
  -n rental-commerce-dev
```

## 운영 명령어

```bash
# 앱 상태 확인
argocd app get rental-commerce-dev
argocd app get rental-commerce-prod

# 수동 동기화 (prod)
argocd app sync rental-commerce-prod

# 롤백 (이전 리비전으로)
argocd app rollback rental-commerce-prod <REVISION>

# diff 확인 (배포 전 미리보기)
argocd app diff rental-commerce-prod

# 앱 히스토리
argocd app history rental-commerce-prod
```

## 디렉토리 구조

```
deploy/
├── argocd/
│   ├── project.yml           # AppProject (RBAC)
│   ├── application-dev.yml   # Dev Application (자동 sync)
│   ├── application-prod.yml  # Prod Application (수동 sync)
│   └── README.md             # 이 문서
└── k8s/
    ├── configmap.yml         # base ConfigMap
    ├── deployment.yml        # base Deployment
    ├── hpa.yml               # base HPA
    ├── ingress.yml           # base Ingress
    ├── kustomization.yml     # base Kustomization
    ├── namespace.yml         # base Namespace
    ├── secret.yml            # base Secret (template)
    ├── service.yml           # base Service
    └── overlays/
        ├── dev/
        │   └── kustomization.yml   # dev 오버라이드 (1 replica, DEBUG)
        └── prod/
            └── kustomization.yml   # prod 오버라이드 (3 replicas, WARN)
```
