#!/usr/bin/env python3
"""
Harness Audit — harness-rules.json 기준 전수 감사.

사용법:
  python3 .claude/scripts/harness-audit.py [--path <root>] [--format text|json]

동작:
  - harness-rules.json 의 forbidden_patterns 전체를 읽음
  - 프로젝트 루트 아래 .kt/.sql 파일을 glob 매칭해 패턴 검사
  - 위반을 '파일:라인  [severity/룰ID]  메시지' 형식으로 출력
  - exit code: 위반 없음 0, error 위반 있음 2, warning 만 있음 1

설계:
  - 훅의 check_code_patterns 와 동일 로직이지만 전체 파일 대상.
  - CI 에서 nightly 로 돌고, false positive 는 룰의 exclude_glob 에 패턴 추가해 제거.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from fnmatch import fnmatch
from pathlib import Path


def load_rules(root: Path) -> dict:
    rules_path = root / ".claude" / "harness-rules.json"
    if not rules_path.exists():
        print(f"harness-rules.json not found at {rules_path}", file=sys.stderr)
        sys.exit(2)
    with open(rules_path, "r", encoding="utf-8") as f:
        return json.load(f)


def iter_target_files(root: Path, file_globs: list[str]) -> list[Path]:
    collected: list[Path] = []
    for pat in file_globs:
        if pat.endswith(".kt"):
            collected.extend(root.rglob("*.kt"))
        elif pat.endswith(".sql"):
            collected.extend(root.rglob("*.sql"))
    # 중복 제거 + 빌드/캐시 디렉토리 제외
    seen: set[Path] = set()
    out: list[Path] = []
    for p in collected:
        if p in seen:
            continue
        seen.add(p)
        if any(part in {"build", ".gradle", "node_modules", ".idea", ".kotlin"} for part in p.parts):
            continue
        out.append(p)
    return out


def match_glob(path: Path, root: Path, globs: list[str]) -> bool:
    filename = path.name
    rel = path.relative_to(root).as_posix()
    return any(fnmatch(filename, g) or fnmatch(rel, g) for g in globs)


def audit(root: Path) -> tuple[list[dict], int]:
    rules = load_rules(root)
    forbidden = rules.get("forbidden_patterns", {}).get("rules", [])
    all_globs = {g.strip() for r in forbidden for g in r.get("file_glob", "*").split(",")}
    files = iter_target_files(root, list(all_globs))

    violations: list[dict] = []
    for rule in forbidden:
        pattern = re.compile(rule["pattern"])
        file_globs = [g.strip() for g in rule.get("file_glob", "*").split(",")]
        exclude_globs = [g.strip() for g in rule.get("exclude_glob", "").split(",") if g.strip()]
        severity = rule.get("severity", "error")
        rule_id = rule["id"]
        message = rule["message"]
        for path in files:
            if not match_glob(path, root, file_globs):
                continue
            if exclude_globs and match_glob(path, root, exclude_globs):
                continue
            try:
                content = path.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            in_block_comment = False
            for i, line in enumerate(content.splitlines(), start=1):
                stripped = line.strip()
                # 블록 주석 상태 추적 (/* ... */)
                if in_block_comment:
                    if "*/" in stripped:
                        in_block_comment = False
                    continue
                if stripped.startswith("/*") and "*/" not in stripped:
                    in_block_comment = True
                    continue
                # 라인 주석 제외 (// ... 또는 -- ...)
                if stripped.startswith("//") or stripped.startswith("--") or stripped.startswith("*"):
                    continue
                # 코드 뒤 인라인 주석은 주석 제거 후 재검사 (Kotlin '//', SQL '--')
                code_only = stripped
                for marker in ("//", "--"):
                    idx = code_only.find(marker)
                    if idx > 0:
                        # 문자열 리터럴 안의 // 는 일단 통과 — 단순 분리
                        code_only = code_only[:idx].rstrip()
                        break
                if not code_only:
                    continue
                if pattern.search(code_only):
                    violations.append(
                        {
                            "file": path.relative_to(root).as_posix(),
                            "line": i,
                            "severity": severity,
                            "rule_id": rule_id,
                            "message": message,
                            "snippet": code_only[:120],
                        }
                    )

    error_count = sum(1 for v in violations if v["severity"] == "error")
    return violations, error_count


def format_text(violations: list[dict]) -> str:
    if not violations:
        return "✅ harness audit: 위반 없음.\n"
    buckets: dict[str, list[dict]] = {}
    for v in violations:
        buckets.setdefault(v["rule_id"], []).append(v)

    lines: list[str] = []
    lines.append(f"⚠ harness audit: {len(violations)}건 위반 (rule 별 집계)")
    lines.append("")
    for rule_id, items in sorted(buckets.items(), key=lambda kv: -len(kv[1])):
        lines.append(f"--- {rule_id}  ({len(items)}건, severity={items[0]['severity']}) ---")
        lines.append(f"  {items[0]['message']}")
        for v in items[:10]:  # 룰당 최대 10건
            lines.append(f"  [{v['severity'].upper()}] {v['file']}:{v['line']}  |  {v['snippet']}")
        if len(items) > 10:
            lines.append(f"  ... +{len(items) - 10} more")
        lines.append("")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--path", default=".", help="프로젝트 루트")
    parser.add_argument("--format", choices=("text", "json"), default="text")
    args = parser.parse_args()

    root = Path(args.path).resolve()
    violations, error_count = audit(root)

    if args.format == "json":
        print(json.dumps(violations, ensure_ascii=False, indent=2))
    else:
        print(format_text(violations))

    if error_count > 0:
        return 2
    if violations:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
