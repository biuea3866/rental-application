#!/usr/bin/env bash
# =============================================================================
# Claude Code PreToolUse(Bash) Hook — gh pr create 차단
# =============================================================================
# gh pr create 감지 시 3단계 검증:
#   1. 브랜치 네이밍 + 1티켓=1PR
#   2. pre-pr-check.sh (forbidden_patterns grep 전체)
#   3. kotlin-reviewer 에이전트 코드 리뷰 (클린코드/디자인패턴/아키텍처)
# 모든 단계 통과해야만 PR 생성 허용.
# =============================================================================

set -euo pipefail

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"

# gh pr create 감지
if echo "$TOOL_INPUT" | grep -qE 'gh\s+pr\s+create'; then
    PROJECT_ROOT="/Users/biuea/feature/flag_project/rental-commerce"
    CHECK_SCRIPT="$PROJECT_ROOT/.claude/pre-pr-check.sh"

    if [ ! -f "$CHECK_SCRIPT" ]; then
        echo "BLOCKED: pre-pr-check.sh not found at $CHECK_SCRIPT"
        exit 2
    fi

    # ─── Step 1: 브랜치 네이밍 + 1티켓=1PR ───
    BRANCH=$(cd "$PROJECT_ROOT" && git branch --show-current 2>/dev/null || echo "unknown")
    if ! echo "$BRANCH" | grep -qE '^feature/RC-(BE|FE|DEVOPS)-[0-9]+$|^fix/|^revert/|^hotfix/|^docs/'; then
        echo "BLOCKED: 브랜치명 '$BRANCH'이 컨벤션을 위반합니다."
        echo "  허용 형식: feature/RC-BE-201, feature/RC-FE-214, fix/..., revert/..., hotfix/..."
        exit 2
    fi

    TICKET_COUNT=$(cd "$PROJECT_ROOT" && git log dev..HEAD --oneline 2>/dev/null | grep -oE 'RC-(BE|FE|DEVOPS)-[0-9]+' | sort -u | wc -l | tr -d ' ')
    if [ "$TICKET_COUNT" -gt 1 ]; then
        echo "BLOCKED: 1 티켓 = 1 PR 원칙 위반. 이 브랜치에 ${TICKET_COUNT}개 티켓이 섞여있습니다."
        exit 2
    fi

    # ─── Step 2: pre-pr-check.sh (grep 패턴 검증) ───
    echo "[PR Gate Step 2/3] pre-pr-check.sh 실행 중..."
    cd "$PROJECT_ROOT"
    if ! bash "$CHECK_SCRIPT"; then
        echo ""
        echo "BLOCKED: pre-pr-check.sh 실패 — 위반 사항 수정 후 다시 시도하세요."
        exit 2
    fi

    # ─── Step 3: kotlin-reviewer 코드 리뷰 강제 ───
    # 변경된 .kt 파일이 있을 때만 리뷰어 실행
    CHANGED_KT=$(cd "$PROJECT_ROOT" && git diff dev --name-only -- '*.kt' | grep -v build | grep -v '.kotlin' || true)
    if [ -n "$CHANGED_KT" ]; then
        echo ""
        echo "[PR Gate Step 3/3] kotlin-reviewer 코드 리뷰 필수"
        echo "========================================"
        echo "  PR 생성 전에 다음 명령으로 코드 리뷰를 수행하세요:"
        echo ""
        echo "  Agent(subagent_type='everything-claude-code:kotlin-reviewer')"
        echo ""
        echo "  리뷰어가 검증하는 항목:"
        echo "    [UseCase 규칙]"
        echo "    - UseCase는 DomainService만 호출 (Repository/Gateway/EventPublisher 직접 참조 절대 금지)"
        echo "    - UseCase execute() 10줄 이내"
        echo "    - UseCase에 if+throw 금지 → Entity.validateXxx()로 캡슐화"
        echo "    - UseCase에 .status.canTransitTo() 금지 → Entity 상태 전이 메서드 내부에 캡슐화"
        echo "    - UseCase에 private fun validate* 금지"
        echo ""
        echo "    [Entity 캡슐화]"
        echo "    - if (entity.isXxx()) throw → entity.validateNotXxx() 캡슐화 (❌ if+throw 나열 금지)"
        echo "    - 상태 전이 검증은 Entity 내부 requireTransitionTo()"
        echo "    - Entity는 순수 (Gateway/Repository/Publisher import 금지)"
        echo ""
        echo "    [클린 코드]"
        echo "    - Guard clause (early return) 필수"
        echo "    - 추상화 수준 일관성 (같은 메서드에 높은/낮은 추상화 섞기 금지)"
        echo "    - if-else 중첩 depth 2 이상 금지"
        echo ""
        echo "    [디자인 패턴]"
        echo "    - if-else/when 분기 3개 이상 → Strategy"
        echo "    - 상태별 행위 분기 → State"
        echo "    - 동일 구조 3개 이상 반복 → Template Method"
        echo "    - 한 이벤트 후속 처리 2개 이상 → Observer"
        echo ""
        echo "    [패키지 격리]"
        echo "    - domain.{A} → domain.{B} import 금지 (domain.common만 허용)"
        echo "    - infrastructure.{A} → infrastructure.{B} import 금지"
        echo "    - application은 교차 참조 허용"
        echo "========================================"
        echo ""
        echo "BLOCKED: kotlin-reviewer 코드 리뷰를 먼저 수행하세요."
        echo "리뷰 통과 후 .claude/review-passed 파일을 생성하면 PR 생성이 허용됩니다."
        echo "  예: echo 'PASS' > .claude/review-passed"
        echo ""

        REVIEW_PASS_FILE="$PROJECT_ROOT/.claude/review-passed"
        if [ -f "$REVIEW_PASS_FILE" ]; then
            echo "[PR Gate] ✅ kotlin-reviewer 리뷰 통과 확인됨"
            rm -f "$REVIEW_PASS_FILE"  # 일회성 — 사용 후 삭제
        else
            exit 2
        fi
    fi

    echo "[PR Gate] ✅ 모든 검증 통과 — PR 생성 허용"
fi
