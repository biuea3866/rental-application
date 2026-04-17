#!/usr/bin/env bash
# =============================================================================
# Claude Code PreToolUse(Agent) Hook — 에이전트 하네스 룰 + 리뷰어 강제
# =============================================================================
# Agent 스폰 시:
#   1. 프롬프트에 harness-rules.json 정독 지시 없으면 BLOCKED
#   2. BE 구현 에이전트는 PR 생성 전 kotlin-reviewer 호출 지시 포함 필수
# =============================================================================

set -euo pipefail

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"

if echo "$TOOL_INPUT" | grep -qE '"prompt"'; then
    # 1. harness-rules.json 정독 지시 확인
    if ! echo "$TOOL_INPUT" | grep -qE 'harness-rules\.json|harness.rules|하네스.*룰|harness.*rule'; then
        echo "BLOCKED: 에이전트 프롬프트에 '.claude/harness-rules.json 정독' 지시가 없습니다."
        echo ""
        echo "  모든 에이전트 프롬프트 첫 줄에 다음을 포함하세요:"
        echo "  '먼저 .claude/harness-rules.json 전체를 읽고 모든 규칙을 숙지하세요.'"
        exit 2
    fi

    # 2. BE 구현 에이전트에 kotlin-reviewer 지시 확인
    if echo "$TOOL_INPUT" | grep -qE 'RC-BE-|UseCase|Domain|Controller|Repository'; then
        if ! echo "$TOOL_INPUT" | grep -qE 'kotlin-reviewer|code-reviewer|리뷰어|reviewer'; then
            echo "BLOCKED: BE 구현 에이전트에 'kotlin-reviewer 코드 리뷰 필수' 지시가 없습니다."
            echo ""
            echo "  BE 에이전트 프롬프트에 다음을 포함하세요:"
            echo "  'PR 생성 전 kotlin-reviewer 에이전트로 코드 리뷰를 수행하고,"
            echo "   리뷰 통과 후 echo PASS > .claude/review-passed 실행 후 gh pr create'"
            exit 2
        fi
    fi
fi
