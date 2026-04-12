#!/bin/bash
# =============================================================================
# PR 생성 전 필수 검증 스크립트
# 사용법: bash .claude/pre-pr-check.sh
# 종료코드: 0=PASS, 1=FAIL (PR 생성 차단)
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

ERRORS=0

echo "=========================================="
echo " PR 생성 전 하네스 검증"
echo "=========================================="

# 1. PR base 확인 (dev여야 함)
echo ""
echo "[1/6] PR base 브랜치 확인..."
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "dev" ]; then
    echo "  ❌ FAIL: feature 브랜치에서 실행하세요 (현재: $CURRENT_BRANCH)"
    ERRORS=$((ERRORS + 1))
else
    echo "  ✅ PASS: $CURRENT_BRANCH"
fi

# 2. FQCN 검사
echo ""
echo "[2/6] FQCN 사용 검사..."
FQCN_FILES=$(git diff dev --name-only -- '*.kt' | grep -v build | grep -v '.kotlin' | xargs grep -l 'com\.rental\.[a-z]\+\.[a-z]\+\.[A-Z][A-Za-z]*(' 2>/dev/null || true)
if [ -n "$FQCN_FILES" ]; then
    echo "  ❌ FAIL: FQCN 사용 발견"
    echo "$FQCN_FILES" | while read f; do
        echo "    - $f"
        grep -n 'com\.rental\.[a-z]\+\.[a-z]\+\.[A-Z][A-Za-z]*(' "$f" | head -3
    done
    ERRORS=$((ERRORS + 1))
else
    echo "  ✅ PASS"
fi

# 3. !! (double-bang) 검사
echo ""
echo "[3/6] !! (double-bang) 검사..."
BANG_FILES=$(git diff dev --name-only -- '*.kt' | grep -v build | grep -v '.kotlin' | xargs grep -ln '\w!!' 2>/dev/null || true)
if [ -n "$BANG_FILES" ]; then
    echo "  ❌ FAIL: !! 사용 발견"
    echo "$BANG_FILES" | while read f; do
        echo "    - $f"
        grep -n '\w!!' "$f" | head -3
    done
    ERRORS=$((ERRORS + 1))
else
    echo "  ✅ PASS"
fi

# 4. @Query 검사
echo ""
echo "[4/6] @Query 어노테이션 검사..."
QUERY_FILES=$(git diff dev --name-only -- '*.kt' | grep -v build | grep -v '.kotlin' | xargs grep -l '@Query(' 2>/dev/null || true)
if [ -n "$QUERY_FILES" ]; then
    echo "  ❌ FAIL: @Query 사용 발견"
    echo "$QUERY_FILES" | while read f; do echo "    - $f"; done
    ERRORS=$((ERRORS + 1))
else
    echo "  ✅ PASS"
fi

# 5. LocalDateTime 검사
echo ""
echo "[5/6] LocalDateTime 검사..."
LDT_FILES=$(git diff dev --name-only -- '*.kt' | grep -v build | grep -v '.kotlin' | grep -v migration | xargs grep -l 'LocalDateTime' 2>/dev/null || true)
if [ -n "$LDT_FILES" ]; then
    echo "  ❌ FAIL: LocalDateTime 사용 발견 (ZonedDateTime 사용 필요)"
    echo "$LDT_FILES" | while read f; do echo "    - $f"; done
    ERRORS=$((ERRORS + 1))
else
    echo "  ✅ PASS"
fi

# 6. UseCase에서 Repository 직접 호출 검사
echo ""
echo "[6/6] UseCase → Repository 직접 호출 검사..."
REPO_IN_UC=$(git diff dev --name-only -- '*UseCase*.kt' | grep -v build | grep -v Test | xargs grep -l 'Repository' 2>/dev/null || true)
if [ -n "$REPO_IN_UC" ]; then
    echo "  ⚠️  WARN: UseCase에서 Repository 직접 참조 — DomainService 경유 권장"
    echo "$REPO_IN_UC" | while read f; do echo "    - $f"; done
    # WARNING only, not blocking for now (gradual migration)
else
    echo "  ✅ PASS"
fi

# 결과
echo ""
echo "=========================================="
if [ $ERRORS -gt 0 ]; then
    echo " ❌ FAIL: $ERRORS건 위반 — PR 생성 차단"
    echo "=========================================="
    exit 1
else
    echo " ✅ PASS: 모든 검증 통과 — PR 생성 가능"
    echo "=========================================="
    exit 0
fi
