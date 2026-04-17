#!/usr/bin/env bash
# =============================================================================
# Claude Code PreToolUse(Bash) Hook — git checkout -b 브랜치명 검증
# =============================================================================
# 새 브랜치 생성 시 feature/{ticket-id} 컨벤션 강제.
# =============================================================================

set -euo pipefail

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"

# git checkout -b 또는 git worktree add ... -b 감지
if echo "$TOOL_INPUT" | grep -qE 'git\s+(checkout\s+-b|worktree\s+add\s+.*-b)\s+'; then
    BRANCH_NAME=$(echo "$TOOL_INPUT" | grep -oE '(checkout\s+-b|worktree\s+add\s+\S+\s+-b)\s+\S+' | awk '{print $NF}')

    if [ -z "$BRANCH_NAME" ]; then
        exit 0  # 추출 실패 시 통과 (오탐 방지)
    fi

    # 허용 패턴: feature/RC-BE-NNN, feature/RC-FE-NNN, feature/RC-DEVOPS-NNN, fix/*, revert/*, hotfix/*
    if ! echo "$BRANCH_NAME" | grep -qE '^feature/RC-(BE|FE|DEVOPS)-[0-9]+$|^fix/|^revert/|^hotfix/|^docs/'; then
        echo "BLOCKED: 브랜치명 '$BRANCH_NAME'이 컨벤션을 위반합니다."
        echo ""
        echo "  허용 형식:"
        echo "    feature/RC-BE-201     (BE 티켓)"
        echo "    feature/RC-FE-214     (FE 티켓)"
        echo "    feature/RC-DEVOPS-211 (DevOps 티켓)"
        echo "    fix/...               (핫픽스)"
        echo "    revert/...            (리버트)"
        echo ""
        echo "  금지: feature/RC-201-rental-domain (설명 suffix), feature/RC-201 (직군 prefix 누락)"
        exit 2
    fi
fi
