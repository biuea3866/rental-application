#!/usr/bin/env bash
# =============================================================================
# Claude Code PreToolUse(Agent) Hook
# =============================================================================
# Agent 스폰 시 3가지 검증:
#   1. harness-rules.json 정독 지시 필수
#   2. BE 구현 에이전트 → kotlin-reviewer 지시 필수
#   3. model=opus 지정 시 아키텍처 키워드 없으면 WARN (sonnet 권장)
# =============================================================================

set -euo pipefail

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"

if echo "$TOOL_INPUT" | grep -qE '"prompt"'; then

    # ─── 1. harness-rules.json 정독 지시 확인 ───
    if ! echo "$TOOL_INPUT" | grep -qE 'harness-rules\.json|harness.rules|하네스.*룰|harness.*rule'; then
        echo "BLOCKED: 에이전트 프롬프트에 '.claude/harness-rules.json 정독' 지시가 없습니다."
        echo ""
        echo "  모든 에이전트 프롬프트 첫 줄에 다음을 포함하세요:"
        echo "  '먼저 .claude/harness-rules.json 전체를 읽고 모든 규칙을 숙지하세요.'"
        exit 2
    fi

    # ─── 2. BE 구현 에이전트 → kotlin-reviewer 지시 필수 ───
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

    # ─── 3. 모델 선택 정책 — opus는 아키텍처 작업에만 ───
    if echo "$TOOL_INPUT" | grep -qE '"model":\s*"opus"'; then
        # opus 지정됨 — 아키텍처/설계/리팩토링 키워드 확인
        if ! echo "$TOOL_INPUT" | grep -qiE 'refactor|리팩|architect|아키텍|design.?pattern|디자인.?패턴|설계|State.?패턴|Strategy|Template.?Method|Observer|Bounded.?Context|BC.?이동|reviewer|리뷰어|동시성|concurrency|성능'; then
            echo ""
            echo "⚠️  WARNING: opus 모델이 지정되었지만 아키텍처/설계 키워드가 감지되지 않습니다."
            echo ""
            echo "  agent_model_policy (harness-rules.json):"
            echo "    sonnet (기본): 단일 티켓 구현, FE, DevOps, 린트 수정, 테스트, 문서"
            echo "    opus (제한):   다중 파일 리팩토링, 디자인 패턴, 도메인 설계, 코드 리뷰"
            echo ""
            echo "  단순 작업이라면 model 파라미터를 'sonnet'으로 변경하세요."
            echo "  아키텍처 판단이 정말 필요하다면 이 경고를 무시하고 진행 가능합니다."
            echo ""
            # WARN만 — BLOCK하지 않음 (판단은 사용자/orchestrator에게)
        fi
    fi
fi
