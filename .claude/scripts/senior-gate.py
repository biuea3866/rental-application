#!/usr/bin/env python3
"""
Senior Gate — pr-senior/tech-lead 자동화 휴리스틱.

pr-reviewer 에이전트가 발견한 실제 Sprint 4 Critical 유형을 CI 에서 저비용으로
선제 차단하는 것이 목적이다.

현재 커버:
  1. Admin Controller 의 @RoleRequired 누락 (PR #100 유형)
  2. Controller 에 HttpServletRequest 직접 주입 (PR #98 유형)
  3. Listener 에 Repository 직접 주입 (PR #104 유형)
  4. UseCase 에 @Transactional 누락 (경고)

입력: stdin 또는 argv[1] 로 개행 분리된 파일 경로 리스트(diff scope).
출력: '[CRITICAL]' / '[MAJOR]' / '[MINOR]' prefix 로 위반 목록.
exit: critical 1건 이상이면 1, 아니면 0 (workflow 는 exit code 별도로 판정).
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


CONTROLLER_SUFFIX = re.compile(r"ApiController(?:\.kt)?$")
ADMIN_PATH_HINT = re.compile(r"/admin/", re.IGNORECASE)
HAS_ROLE_REQUIRED = re.compile(r"@RoleRequired\s*\(\s*\"ADMIN\"")
HAS_HTTP_SERVLET = re.compile(r"HttpServletRequest\s+\w+|\w+:\s*HttpServletRequest")
LISTENER_SUFFIX = re.compile(r"Listener\.kt$")
REPO_IN_CONSTRUCTOR = re.compile(r"private\s+val\s+\w+(?:Repository|JpaRepository)")
USECASE_FILE = re.compile(r"UseCase\.kt$")
HAS_TRANSACTIONAL = re.compile(r"@Transactional")


def scan(files: list[Path]) -> list[str]:
    findings: list[str] = []
    for path in files:
        if not path.exists() or not path.is_file():
            continue
        try:
            src = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        fname = path.name

        # 1) Admin Controller + @RoleRequired("ADMIN") 누락
        if CONTROLLER_SUFFIX.search(fname) and (ADMIN_PATH_HINT.search(src) or "/admin" in src):
            if not HAS_ROLE_REQUIRED.search(src):
                findings.append(
                    f"[CRITICAL] {path}: Admin 경로를 가진 Controller 에 @RoleRequired(\"ADMIN\") 누락 — PR #100 재발 방지"
                )

        # 2) Controller 에 HttpServletRequest 직접 주입
        if CONTROLLER_SUFFIX.search(fname) and HAS_HTTP_SERVLET.search(src):
            findings.append(
                f"[MAJOR] {path}: Controller 가 HttpServletRequest 를 직접 주입받음 — "
                f"ArgumentResolver (@AuthenticatedMember / @AuthenticatedRole) 패턴으로 교체 권장"
            )

        # 3) Listener 에 Repository 직접 주입
        if LISTENER_SUFFIX.search(fname) and REPO_IN_CONSTRUCTOR.search(src):
            findings.append(
                f"[CRITICAL] {path}: Listener 가 Repository 를 직접 주입받음 — "
                f"DomainService 경유로 이동 (PR #104 → #112 refactor 재발 방지)"
            )

        # 4) UseCase 에 @Transactional 누락 (@Service class 기반으로 단순 체크)
        if USECASE_FILE.search(fname) and "@Service" in src and not HAS_TRANSACTIONAL.search(src):
            findings.append(
                f"[MAJOR] {path}: @Service UseCase 에 @Transactional 누락 — "
                f"execute() 트랜잭션 경계 명시 필요"
            )

    return findings


def parse_files_arg(arg: str) -> list[Path]:
    return [Path(line.strip()) for line in arg.splitlines() if line.strip()]


def main() -> int:
    if len(sys.argv) >= 2:
        files = parse_files_arg(sys.argv[1])
    else:
        files = parse_files_arg(sys.stdin.read())

    if not files:
        print("senior-gate: diff 범위 없음 (Kotlin/SQL 파일 변경 0건)")
        return 0

    findings = scan(files)
    if not findings:
        print(f"✅ senior-gate: {len(files)}개 파일 검사 — 이상 없음")
        return 0

    print(f"⚠ senior-gate: {len(findings)}건 발견 ({len(files)}개 파일 검사)")
    print()
    for f in findings:
        print(f)

    critical = sum(1 for f in findings if f.startswith("[CRITICAL]"))
    return 1 if critical > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
