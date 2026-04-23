#!/usr/bin/env python3
"""
QA Follow-up Ticket 추출기.

docs/qa/*.md 의 "## 후속 티켓 제안" 또는 "### 후속 티켓" 섹션을 파싱해
| RC-BE-4xx | 설명 | 근거 | 형식 테이블에서 티켓 목록을 JSON 으로 출력.

테이블 헤더는 유연하게 인식:
  | 티켓 ... | 근거 ... |
  | RC-BE-441 ... | pr-reviewer ... |
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


TICKET_CODE = re.compile(r"RC-[A-Z]+-\d+")
SECTION_HEADING_PATTERNS = [
    re.compile(r"^#{2,3}\s+(?:\d+\.\s+)?(추가\s+)?후속\s+티켓"),
    re.compile(r"^#{2,3}\s+Follow-up Tickets", re.IGNORECASE),
]


def extract(report_path: Path) -> list[dict]:
    if not report_path.exists():
        return []
    text = report_path.read_text(encoding="utf-8")
    # 섹션 찾기
    section = None
    lines = text.splitlines()
    for i, line in enumerate(lines):
        if any(p.match(line.strip()) for p in SECTION_HEADING_PATTERNS):
            section = i
            break
    if section is None:
        return []

    tickets: list[dict] = []
    # 이후 줄에서 | RC-xxx | ... | 형식 파싱
    for line in lines[section + 1 :]:
        if line.startswith("## ") and not any(p.match(line.strip()) for p in SECTION_HEADING_PATTERNS):
            break
        if not line.strip().startswith("|"):
            continue
        m = TICKET_CODE.search(line)
        if not m:
            continue
        code = m.group(0)
        parts = [p.strip() for p in line.strip().strip("|").split("|")]
        if len(parts) < 1:
            continue
        # 첫 셀: 티켓 코드 + 요약 (예: "RC-BE-441 Presentation 통합 테스트 — ...")
        first_cell = parts[0]
        summary = first_cell.replace(code, "").strip(" -—:")
        source = parts[1] if len(parts) > 1 else ""
        detail = " | ".join(parts[1:]) if len(parts) > 1 else summary
        tickets.append(
            {
                "code": code,
                "summary": summary,
                "source": source,
                "detail": detail,
            }
        )
    return tickets


def main() -> int:
    if len(sys.argv) < 2:
        print("[]")
        return 0
    report_path = Path(sys.argv[1])
    tickets = extract(report_path)
    print(json.dumps(tickets, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
